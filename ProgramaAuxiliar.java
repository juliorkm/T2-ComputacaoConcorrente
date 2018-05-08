import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

class Assento {
	private boolean reservado;
	private int position;
	private int threadId;
	
	public Assento(int position, int id) {
		this.position = position;
		this.threadId = id;
		this.reservado = (id == 0) ? false : true;
	}
	
	public int getPosition() {
		return this.position;
	}

	public int getThreadId() {
		return this.threadId;
	}
	
	public int viewSeat() {
		if (this.reservado) return this.threadId;
		else return 0;
	}
	
	public boolean setOwner(int id) {
		if (this.reservado)
			return false;

		this.reservado = true;
		this.threadId = id;
		return true;
	}
	
	public boolean freeSeat(int id) {
		if (this.reservado && this.threadId == id) {
			this.reservado = false;
			return true;
		}
		else return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!o.getClass().equals(Assento.class)) return false;
		Assento a = (Assento) o;
		if (this.position == a.getPosition() && this.viewSeat() == a.viewSeat()) return true;
		else return false;
	}
	
	@Override
	public String toString() {
		return ("(" + reservado + "," + position + "," + threadId + ")");
	}
}

public class ProgramaAuxiliar {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		ArrayList<Assento> assentos = new ArrayList<Assento>();
		Scanner arquivo, linha;
		int funcao, thread, assento = 0;
		boolean notIniti = true;
		if (args.length < 1) {
			System.err.println("Use ProgramaAuxiliar <log de saída>");
			System.exit(1);
		}
		try {
			arquivo = new Scanner(new File(args[0]));
		
		while (arquivo.hasNextLine()) {
			try {
				ArrayList<Assento> assentos_aux = new ArrayList<Assento>();
				linha = new Scanner(arquivo.nextLine()).useDelimiter(",|\\]|\\[");
				funcao = linha.nextInt();
				thread = linha.nextInt();
				if (funcao != 1) {
					assento = linha.nextInt() - 1;
				}
				linha.next(); //Pula espaço entre a vírgula e o colchete
				int i = 0;
				while (linha.hasNextInt()) {
					if (notIniti) {
						assentos.add(new Assento(i, 0));
					}
					assentos_aux.add(new Assento(i++, linha.nextInt()));
				}
				if (notIniti) notIniti = false;
				if (!escolheFuncao(funcao, thread, assento, assentos).equals(assentos_aux)) {
					if (funcao == 1) erro("Passo errado na linha 1," + thread);
					else erro("Passo errado na linha " + funcao + "," + thread + "," + assento);
				}
				linha.close();
			}
			catch (Exception e) {e.printStackTrace();erro("Formato errado");}
		}
		arquivo.close();
		} catch (FileNotFoundException e) {
			erro("Falha ao abrir arquivo");
		}
		System.out.println();
		System.out.println("Log de saída correto!");
	}
	private static void erro(String erro) {
		System.err.println("Erro: " + erro);
		System.exit(1);
	}
	private static void visualiza(int thread, ArrayList<Assento> array) {
		System.out.println("Avaliando: visualizaAssentos(" + thread + ")");		
	}
	private static ArrayList<Assento> insereRand(int thread, int assento, ArrayList<Assento> array) {
		if (assento < 0) {
			System.out.println("Avaliando: alocaAssentoLivre(" + thread + "), que não encontrou assento livre");
			return array;
		}
		System.out.println("Avaliando: alocaAssentoLivre(" + thread + "), que alocou o assento #" + (assento+1));
		if (array.get(assento).viewSeat() != 0) erro("Tentou alocar aleatoriamente um não-disponível");
		else array.set(assento, new Assento(assento, thread));
		return array;
	}
	private static ArrayList<Assento> insereDado(int thread, int assento, ArrayList<Assento> array) {
		System.out.println("Avaliando: alocaAssentoDado(assento_de_numero_" + (assento+1) + "," + thread + ")");
		if (assento < 0 || assento > array.size()) return array;
		if (array.get(assento) == null || array.get(assento).viewSeat() != 0) return array;
		else array.set(assento, new Assento(assento, thread));
		return array;
	}
	private static ArrayList<Assento> remove(int thread, int assento, ArrayList<Assento> array) {
		System.out.println("Avaliando: liberaAssento(assento_de_numero_" + (assento+1) + "," + thread + ")");
		if (assento < 0 || assento > array.size()) return array;
		if (array.get(assento) == null || array.get(assento).getThreadId() != thread) return array;
		else array.set(assento, new Assento(assento, 0));
		return array;
	}
	private static ArrayList<Assento> escolheFuncao(int funcao, int thread, int assento, ArrayList<Assento> array) {
		switch (funcao) {
		case 1:
			visualiza(thread, array);
			break;
		case 2:
			array = insereRand(thread, assento, array);
			break;
		case 3:
			array = insereDado(thread, assento, array);
			break;
		case 4:
			array = remove(thread, assento, array);
			break;
		default:
			erro("Função inválida");
		}
		return array;
	}
}
