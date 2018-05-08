/* 	Trabalho 2 - Computação Concorrente
	Nome: Ingrid Quintanilha Pacheco || DRE: 115149161
	Nome: Júlio Rama Krsna Mandoju || DRE: 115023797
	Professora: Silvana Rossetto
*/

import java.util.ArrayList;
import java.util.Random;
import java.io.*;

class LE{
	private int nleitor;
	private int nescritor;
	private boolean espera;
	
	public LE(){
		nleitor = 0;
		nescritor = 0;
		espera = false;
	}
	
	public synchronized void entraEscritor(){
		while (nleitor != 0 || nescritor != 0){
			espera = true;
			try {this.wait();}
			catch (InterruptedException e) {
				System.err.println("Erro de sincronização.");
				System.exit(0);
			}
		}
		espera = false;
		nescritor++;
	}
	
	public synchronized void saiEscritor(){
		nescritor--;
		if (nescritor == 0){
			this.notifyAll();
		}
	}

	public synchronized void entraLeitor(){
		while (nescritor != 0 || espera){
			try {this.wait();}
			catch (InterruptedException e) {
				System.err.println("Erro de sincronização.");
				System.exit(0);
			}
		}
		nleitor++;
	}

	public synchronized void saiLeitor(){
		nleitor--;
		if (nleitor == 0){
			this.notify();
		}
	}
}

class Buffer {
	private String b[];
	private int in, out;
	private int contador;
	private int max;

	public Buffer(int size){
		max = size;
		b = new String[max];
		in = 0;
		out = 0;
		contador = 0;
	}

	public synchronized int getContador() {
		return this.contador;
	}
	
	public synchronized void insere(String str){
		try{
			while (contador == max){
				this.wait();
			}
			b[in] = str;
			in = (in + 1) % max;
			contador++;
			if (contador == 1) this.notify();
		}
		catch(InterruptedException e){
			System.err.println("Erro ao inserir elemento no buffer");
			System.exit(0);
		}
	}

	public synchronized void imprime(){
		for (int i = 0; i < max; i++){
			System.out.println(b[i]);
		}
	}

	public synchronized String retira(){
		try{
			String aux = new String();
			while (contador == 0){
				this.wait();
			}
			aux = b[out];
			//b[out] = "#";
			out = (out + 1) % max;
			contador--;
			if (contador == max - 1) this.notifyAll();
			return aux;
		}
		catch(InterruptedException e){
			System.err.println("Erro ao inserir elemento no buffer");
			System.exit(0);
			return null;
		}
	}
}

class t_Assento {
	private boolean reservado;
	private int position;
	private int threadId;
	
	public t_Assento(int position) {
		this.reservado = false;
		this.position = position;
		this.threadId = 0;
	}
	
	public int getPosition() {
		return this.position;
	}

	public int getThreadId() {
		return this.threadId;
	}
	
	public synchronized int viewSeat() {
		if (this.reservado) return this.threadId;
		else return 0;
	}
	
	public synchronized boolean setOwner(int id) {
		if (this.reservado)
			return false;

		this.reservado = true;
		this.threadId = id;
		return true;
	}
	
	public synchronized boolean freeSeat(int id) {
		if (this.reservado && this.threadId == id) {
			this.reservado = false;
			return true;
		}
		else return false;
	}
}

class t_Assentos {
	private ArrayList<t_Assento> reservados;
	private ArrayList<t_Assento> disponiveis;
	private ArrayList<t_Assento> assentos;
	
	public t_Assentos() {
		this.reservados = new ArrayList<t_Assento>();
		this.disponiveis = new ArrayList<t_Assento>();

		for (int i = 0; i < Globais.N; i++) {
			disponiveis.add(new t_Assento(i));
		}
		this.assentos = new ArrayList<t_Assento>(disponiveis);
	}

	/**
	 * Função que pega um assento não aleatório baseado em uma seed.
	 * Feita para ser utilizada em t_Assentos.alocaAssentoDado.
	 * @param seed Um valor inteiro para ser usado no cálculo.
	 */
	public t_Assento pegaAssento(int seed) {
		return assentos.get(seed % assentos.size());
	}

	/**
	 * Função usada para montar o mapa de assentos em forma de String.
	 * @return
	 */
	public String retornaMapa(){
		StringBuilder sb = new StringBuilder("[");
		
		for (int i = 0; i < assentos.size()-1; i++) {
			sb.append(assentos.get(i).viewSeat() + ",");
		}
		sb.append(assentos.get(assentos.size()-1).viewSeat() + "]");
		return sb.toString();
	}

	public void visualizaAssentos(int id){
		StringBuilder sb = new StringBuilder(1 + ",");
		sb.append(id + ",");
		Globais.le.entraLeitor();
		sb.append(retornaMapa());
		Globais.buf.insere(sb.toString());
		Globais.le.saiLeitor();
	}
	
	public t_Assento alocaAssentoLivre(int id) {
		t_Assento assLivre = null;
		Random rand = new Random();
		StringBuilder sb = new StringBuilder(2 + ",");
		sb.append(id + ",");

		Globais.le.entraEscritor();
		if (disponiveis.size() > 0) {
			int i = rand.nextInt(disponiveis.size());
			assLivre = disponiveis.get(i);
			sb.append((assLivre.getPosition()+1) + ",");
			assLivre.setOwner(id);
			reservados.add(assLivre);
			disponiveis.remove(i);
		}
		else {
			sb.append("0,");
		}
		sb.append(retornaMapa());
		Globais.buf.insere(sb.toString());

		if (assLivre != null)
			for (int i = 0; i < assentos.size(); i++) {
				if (assentos.get(i).getPosition() == assLivre.getPosition()) {
					assentos.set(i, assLivre);
					break;
				}
			}
		
		Globais.le.saiEscritor();

		return assLivre;
		//Como não existe passagem por referência em Java, precisamos retornar o assento alocado
		//O teste se a função foi bem sucedida, por sua vez, passa a ser o teste se o assento retornado é nulo ou não
	}
	
	public boolean alocaAssentoDado(t_Assento ass, int id) {
		boolean retorno;
		StringBuilder sb = new StringBuilder(3 + ",");
		sb.append(id + ",");
		sb.append((ass.getPosition()+1) + ",");
		Globais.le.entraEscritor();
		if (retorno = ass.setOwner(id)) {
			reservados.add(ass);
			disponiveis.remove(ass);
		}
		sb.append(retornaMapa());
		Globais.buf.insere(sb.toString());

		for (int i = 0; i < assentos.size(); i++) {
			if (assentos.get(i).getPosition() == ass.getPosition()) {
				assentos.set(i, ass);
				break;
			}
		}
		Globais.le.saiEscritor();
		return retorno;
	}
	
	public boolean liberaAssento(t_Assento ass, int id) {
		boolean retorno;
		StringBuilder sb = new StringBuilder(4 + ",");
		sb.append(id + ",");
		if (ass == null) ass = new t_Assento(-1);
		sb.append((ass.getPosition()+1) + ",");
		Globais.le.entraEscritor();
		if (retorno = ass.freeSeat(id)) {
			reservados.remove(ass);
			disponiveis.add(ass);
		}
		sb.append(retornaMapa());
		Globais.buf.insere(sb.toString());

		for (int i = 0; i < assentos.size(); i++) {
			if (assentos.get(i).getPosition() == ass.getPosition()) {
				assentos.set(i, ass);
				break;
			}
		}
		Globais.le.saiEscritor();
		return retorno;
	}
}

class Consumidora extends Thread {
	private PrintWriter out;
	//Bob, o Construtor
	public Consumidora(String arq) {
		try {out = new PrintWriter(arq);}
		catch(IOException e){
			System.err.println("Erro de IO");
			System.exit(1);
		}
	}

	@Override
	public void run(){
		while(true) {
			String aux = Globais.buf.retira();
			out.append(aux + "\n");
			System.out.println(aux);
			if (Globais.threads == 1 && Globais.buf.getContador() == 0) break;
		} 
		out.close();
	}
}

class thread1 extends Thread {
	private int tid;
	private t_Assentos assentos;

	//Bob, o Construtor
	public thread1(int tid, t_Assentos assentos) {
		this.tid = tid;
		this.assentos = assentos;
	}

	@Override
	public void run() {
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		assentos.alocaAssentoLivre(tid);
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		
		synchronized (Globais.threads) {
			Globais.threads--;
		}
	}
}

class thread2 extends Thread {
	private int tid;
	private t_Assentos assentos;
	private t_Assento ass;

	//Bob, o Construtor
	public thread2(int tid, t_Assentos assentos, t_Assento ass) {
		this.tid = tid;
		this.assentos = assentos;
		this.ass = ass;
	}

	@Override
	public void run() {
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		assentos.alocaAssentoDado(ass, tid);
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		
		synchronized (Globais.threads) {
			Globais.threads--;
		}
	}
}

class thread3 extends Thread {
	private int tid;
	private t_Assentos assentos;

	//Bob, o Construtor
	public thread3(int tid, t_Assentos assentos) {
		this.tid = tid;
		this.assentos = assentos;
	}

	@Override
	public void run() {
		t_Assento aux;
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		aux = assentos.alocaAssentoLivre(tid);
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		assentos.liberaAssento(aux, tid);
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		
		synchronized (Globais.threads) {
			Globais.threads--;
		}
	}
}

class thread4 extends Thread {
	private int tid;
	private t_Assentos assentos;

	//Bob, o Construtor
	public thread4(int tid, t_Assentos assentos) {
		this.tid = tid;
		this.assentos = assentos;
	}

	@Override
	public void run() {
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		for(int i=0;i<3;i++) {
			assentos.alocaAssentoLivre(tid);
			assentos.visualizaAssentos(tid);
			assentos.visualizaAssentos(tid);
		}
		
		synchronized (Globais.threads) {
			Globais.threads--;
		}
	}
}

class thread5 extends Thread {
	private int tid;
	private t_Assentos assentos;

	//Bob, o Construtor
	public thread5(int tid, t_Assentos assentos) {
		this.tid = tid;
		this.assentos = assentos;
	}

	@Override
	public void run() {
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		assentos.alocaAssentoLivre(tid);
		assentos.visualizaAssentos(tid);
		assentos.visualizaAssentos(tid);
		for (int i = 0; i < 3; i++) {
			assentos.liberaAssento(assentos.pegaAssento(i*tid+tid), tid);
			assentos.visualizaAssentos(tid);
			assentos.visualizaAssentos(tid);
		}
		
		synchronized (Globais.threads) {
			Globais.threads--;
		}
	}
}
/**
 * Classe onde são guardadas variáveis que devem ser inicializadas na main e
 * acessadas por múltiplas threads.
 */
class Globais {
	public static int N;
	public static Buffer buf;
	public static LE le;
	public static Integer threads;
}

public class Trabalho2 {
	public static void main(String[] args) {
    	String arquivo;
		Globais.buf = new Buffer(50);
		Globais.le = new LE();
		Thread[] threads = new Thread[51];
		Globais.threads = 51;
		
		if (args.length < 2) {
			System.err.println("Erro: Use Trabalho2 <arquivo de saída> <número de assentos>");
			System.exit(1);
		}
		
		arquivo = args[0];
		Globais.N = Integer.parseInt(args[1]);
		t_Assentos assentos = new t_Assentos();

		threads[0] = new Consumidora(arquivo);
		
		for (int i=1; i<11; i++) {
			threads[i] = new thread1(i, assentos);
		}

		for (int i=11; i<21; i++) {
			threads[i] = new thread2(i, assentos, assentos.pegaAssento(i));
		}

		for (int i=21; i<31; i++) {
			threads[i] = new thread3(i, assentos);
		}

		for (int i=31; i<41; i++) {
			threads[i] = new thread4(i, assentos);
		}
		
		for (int i=41; i<51; i++) {
			threads[i] = new thread5(i, assentos);
		}

		for (int i=0; i<51; i++) threads[i].start();
	}
}