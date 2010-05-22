import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class Visualizer {
	// Tournament info
	static final String[] programs = {"random.exe", "9000.exe", "bruteforce.exe"};
	static int[] wins;
	static final int matches = 1; // Number of matches in round-robin tournament with each competitor
	String[] progNames; // Names of all programs
	// Game variables
	final int S = 10;		//board size
	String[] names = new String[2];
	int[][][] ships = new int[2][10][2];
    int[][] lastShots = new int[2][2];
	final int[][] dir = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
	final int[] lengths = {5, 4, 3, 3, 2};
	int[][][] grid = new int[2][S][S];
	final static boolean debug = false;
        // ----------------------------------------
	final int L = 32; // Length of square
	final int G = 20; // Gap width
	final int T = 40; // Text height
	final int W = 2*S*L + 4*G;
	final int H = L*S + 3*G + T;
	// -----------------------------------------
	private int[] parse(String s) {
		int[] output = new int[2];
		output[0] = s.charAt(0) - 'A';
		if(s.length() == 3) {
			output[1] = 9;
		} else {
			output[1] = s.charAt(1) - '1';
		}
		if(debug) {
			System.out.println("Parsing '" + s + "' as " + output[0] + " " + output[1]);
		}
		return output;
	}
	// -----------------------------------------
	// direction from a to b
	private int direction(int[] a, int[] b) throws Exception {
		int dx = b[0] - a[0];
		int dy = b[1] - a[1];
		if(dx != 0 && dy != 0) {
			throw new Exception("Fail");
		}
		if(dx == 0) {
			if(dy > 0) {
				return 1;
			} else if(dy < 0) {
				return 3;
			} else {
				throw new Exception("Fail");
			}
		} else {
			if(dx > 0) {
				return 0;
			} else {
				return 2;
			}
		}
	}
	// -----------------------------------------
	public int runTest() throws IOException {
		int winner = -1;
		try {
			init();
			for(int i=0; i < 4; i++) {
				lastShots[i&1][i>>1] = -1;
			}
			//get names
			StringBuffer sb = new StringBuffer();
			sb.append("name\n");
			for(int i=0; i < 2; i++) {
				os[i].write(sb.toString().getBytes());
				os[i].flush();
				names[i] = br[i].readLine();
				if(debug) {
					System.out.println("Player " + (i+1) + " name: " + names[i]);
				}
			}
			//get ships
			sb = new StringBuffer();
			sb.append("place\n");
			for(int i=0; i < 2; i++) {
				os[i].write(sb.toString().getBytes());
				os[i].flush();
				String locs = br[i].readLine();
				StringTokenizer st = new StringTokenizer(locs);
				int k = 0;
				while(st.hasMoreTokens()) {
					String t = st.nextToken();
					if(k >= 10) {
						winner = 1 - i;
						throw new Exception("Too many ships");
					}
					winner = 1 - i;
					int[] u = parse(t);
					winner = -1;
					if(u[0] < 0 || u[0] >= S || u[1] < 0 || u[1] >= S) {
						winner = 1 - i;
						throw new Exception("Invalid point");
					}
					ships[i][k++] = u;
				}
				if(k < 10) {
					winner = 1 - i;
					throw new Exception("Too few ships");
				}
				//check validity of placement
				for(int j = 0; j < 10; j += 2) {
					int dx = ships[i][j][0] - ships[i][j+1][0];
					int dy = ships[i][j][1] - ships[i][j+1][1];
					if(dx != 0 && dy != 0) {
						winner = 1 - i;
						throw new Exception("Ship not horizontal/vertical");
					}
					int d = dx + dy;
					if(d < 0) d = -d;
					d++;
					if(d != lengths[j / 2]) {
						winner = 1 - i;
						throw new Exception("Invalid ship length");
					}
				}
				for(int j = 0; j < 10; j += 2) {
					int d = direction(ships[i][j], ships[i][j+1]);
					int l = lengths[j / 2];
					for(int m=0; m < l; m++) {
						int r = ships[i][j][0] + m * dir[d][0];
						int c = ships[i][j][1] + m * dir[d][1];
						if(grid[i][r][c] != 0) {
							winner = 1 - i;
							throw new Exception("Overlapping ships");
						}
						grid[i][r][c] = j + 2;
					}
				}
			}
			int turn = 0;
			int[] remaining = {17, 17};
			int[][] hits = new int[2][5];
			for(int turns = 0; turns < 400; turns++) {
				String command = "fire\n";
				os[turn].write(command.getBytes());
				os[turn].flush();
				String s0 = br[turn].readLine();
				StringTokenizer st = new StringTokenizer(s0);
				String s = st.nextToken();
				winner = 1 - turn;
				int[] a = parse(s);
				winner = -1;
				if(a[0] < 0 || a[0] >= S || a[1] < 0 || a[1] >= S) {
					winner = 1 - turn;
					throw new Exception("Invalid fire");
				}
    	        lastShots[turn] = a;
				int hit = 0;
				if(grid[1 - turn][a[0]][a[1]] % 2 == 0) {
					if(grid[1 - turn][a[0]][a[1]] > 0) {
						int ship = grid[1 - turn][a[0]][a[1]] / 2 - 1;
						remaining[1 - turn]--;
						hits[1 - turn][ship]++;
						hit = ship + 1;
					}
					grid[1 - turn][a[0]][a[1]]++;
				}
				draw();
				if(debug) {
					String chars = ".,axbxcxdxex";
					System.out.println(names[0] + "\t" + names[1]);
					for(int r = 0; r < S; r++) {
						for(int i = 0; i < 2; i++) {
							for(int c = 0; c < S; c++) {
								System.out.print(chars.charAt(grid[i][r][c]));
							}
							System.out.print("        ");
						}
						System.out.println();
					}
					System.out.println();
					System.out.println();
				}
				command = "hits\n";
				os[turn].write(command.getBytes());
				os[turn].flush();
				command = "" + hit + "\n";
				os[turn].write(command.getBytes());
				os[turn].flush();
				if(remaining[1 - turn] == 0) {
					winner = turn;
					command = "exit\n";
					os[0].write(command.getBytes());
					os[0].flush();
					os[1].write(command.getBytes());
					os[1].flush();
					break;
				}
				turn = 1 - turn;
			}
			return winner;
		} catch (Exception e) {
			System.err.println("An exception occurred.");
			e.printStackTrace();
			return winner;
		} finally {
			  terminate();
		}
	}
	public void tournament() throws IOException {
		if (vis) {
			jf.setSize(W,H);
			v.repaint();
			jf.setVisible(true);
		}
		int N = programs.length;
		if(N < 2) {
			return;
		}
		int K = N * (N-1) / 2;
		wins = new int[N];
		Random r = new Random();
		r.setSeed(new Date().getTime());
		progNames = new String[N];
		for(int m = 0; m < matches; m++) {
			int[][] matchups = new int[K][2];
			int a = 0;
			for(int i=0; i < N; i++) {
				for(int j=0; j < i; j++) {
					matchups[a][0] = i;
					matchups[a][1] = j;
					int k = r.nextInt(2);
					if(k == 1) {
						matchups[a][0] = j;
						matchups[a][1] = i;
					}
					a++;
				}
			}
			// Shuffle matches
			for(int i = K-1; i > 0; i--) {
				int t = r.nextInt(i);
				int[] x = matchups[t];
				matchups[t] = matchups[i];
				matchups[i] = x;
			}
			// Now we play
			for(int i=0; i < K; i++) {
				prog1 = programs[matchups[i][0]];
				prog2 = programs[matchups[i][1]];
				unterminate();
				int k = runTest();
				terminate();
				try {
					Thread.sleep(del2);
				} catch(Exception e) {}
				if(progNames[matchups[i][0]] == null) {
					progNames[matchups[i][0]] = names[0];
				}
				if(progNames[matchups[i][1]] == null) {
					progNames[matchups[i][1]] = names[1];
				}
				if(k == 0) {
					wins[matchups[i][0]]++;
				} else if(k == 1) {
					wins[matchups[i][1]]++;
				} else {
					System.out.println("Tie\n");
				}
			}
		}
		// Output results to a JFrame
		/**
		Todo:
		- Add JLabels to JFrame
		- Make it not look like shit
		- Add actionListener to button to close window when dismissed
		*/
		JFrame resultsFrame = new JFrame();
		resultsFrame.setTitle("Tournament Results");
		resultsFrame.setResizable(false);
		resultsFrame.setSize(800, 600);
		JLabel[] resultsLabel = new JLabel[N];
		for(int i = 0; i < N; i++) {
			resultsLabel[i] = new JLabel(progNames[i] + " won " + wins[i] + " games.");
			System.out.println(progNames[i] + " won " + wins[i] + " games.");
		}
		resultsFrame.setVisible(true);
	}
	public void unterminate() {
		if (prog1 != null) {
			try {
				Runtime rt = Runtime.getRuntime();
				proc1 = rt.exec(prog1);
				os[0] = proc1.getOutputStream();
				is[0] = proc1.getInputStream();
				br[0] = new BufferedReader(new InputStreamReader(is[0]));
				new ErrorReader(proc1.getErrorStream()).start();
			} catch (Exception e) { 
				e.printStackTrace(); 
			}
		}
		if (prog2 != null) {
			try {
				Runtime rt = Runtime.getRuntime();
				proc2 = rt.exec(prog2);
				os[1] = proc2.getOutputStream();
				is[1] = proc2.getInputStream();
				br[1] = new BufferedReader(new InputStreamReader(is[1]));
				new ErrorReader(proc2.getErrorStream()).start();
			} catch (Exception e) { 
				e.printStackTrace(); 
			}
		}
	}
	public void terminate() {
		if (proc1 != null) {
			try { 
				proc1.destroy(); 
			} catch (Exception e) { 
				e.printStackTrace(); 
			}
		}
		if (proc2 != null) {
			try { 
				proc2.destroy();
			} catch (Exception e) { 
				e.printStackTrace(); 
			}
		}
	}
// ------------- visualization part ------------
	static String prog1, prog2;
	static boolean vis;
	static Process proc1, proc2;
	static int del, del2;
	InputStream[] is;
	OutputStream[] os;
	BufferedReader[] br;
	JFrame jf;
	Vis v;
	// -----------------------------------------
	int init() throws IOException {
		grid = new int[2][10][10];
		return 0;
	}
	// -----------------------------------------
	void draw() {
		if (!vis) return;
		v.repaint();
		if (del>0) {
			try { 
				Thread.sleep(del); 
			} catch (Exception e) { 
				e.printStackTrace(); 
			}
		}
	}
	// -----------------------------------------
	public class Vis extends JPanel implements WindowListener {
		Color[] colors;
		public void paint(Graphics g) {
			//do painting here
			BufferedImage im = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = (Graphics2D)im.getGraphics();
			//background
			g2.setColor(Color.LIGHT_GRAY);
			g2.fillRect(0,0,W,H);
			// names
			g2.setColor(Color.BLACK);
			g2.setFont(new Font("SansSerif", Font.PLAIN, 24));
			if(names[0] != null) {
				g2.drawString(names[0], G, T - 10);
			}
			if(names[1] != null) {
				g2.drawString(names[1], G + L*S + 2*G, T - 10);
			}
			// grids
			int x0 = G;
			int y0 = T;
			for(int i=0; i < S; i++) {
				for(int j=0; j < S; j++) {
					g2.setColor(colors[grid[0][i][j] / 2]);
					g2.fillRect(x0 + j*L, y0 + i*L, L, L);
					g2.setColor(Color.BLACK);
					g2.drawRect(x0 + j*L, y0 + i*L, L, L);
					if(grid[0][i][j] % 2 == 1) {
						if(i == lastShots[1][0] && j == lastShots[1][1]) {
							g2.fillOval(x0 + j*L + L/4, y0 + i*L + L/4, L/2, L/2);
						} else {
							g2.drawOval(x0 + j*L + L/4, y0 + i*L + L/4, L/2, L/2);
						}
					}
				}
			}
			x0 = G + L*S + 2*G;
			y0 = T;
			for(int i=0; i < S; i++) {
				for(int j=0; j < S; j++) {
					g2.setColor(colors[grid[1][i][j] / 2]);
					g2.fillRect(x0 + j*L, y0 + i*L, L, L);
					g2.setColor(Color.BLACK);
					g2.drawRect(x0 + j*L, y0 + i*L, L, L);
					if(grid[1][i][j] % 2 == 1) {
						if(i == lastShots[0][0] && j == lastShots[0][1]) {
							g2.fillOval(x0 + j*L + L/4, y0 + i*L + L/4, L/2, L/2);
						} else {
							g2.drawOval(x0 + j*L + L/4, y0 + i*L + L/4, L/2, L/2);
						}
					}
				}
			}
			g.drawImage(im,0,0,W,H,null);
		}
		public Vis() {
			jf.addWindowListener(this);
			colors = new Color[6];
			colors[0] = new Color(64, 128, 128);
			colors[1] = new Color(70, 204, 36);
			colors[2] = new Color(250, 238, 20);
			colors[3] = new Color(170, 70, 230);
			colors[4] = new Color(255, 151, 47);
			colors[5] = new Color(30, 68, 217);
		}
		//WindowListener
		public void windowClosing(WindowEvent e){
			if(proc1 != null) {
				try { 
					proc1.destroy(); 
				} catch (Exception ex) { 
					ex.printStackTrace(); 
				}
			}
			if(proc2 != null) {
				try { 
					proc2.destroy(); 
				} catch (Exception ex) { 
					ex.printStackTrace(); 
				}
			}
			System.exit(0);
		}
		public void windowActivated(WindowEvent e) { }
		public void windowDeactivated(WindowEvent e) { }
		public void windowOpened(WindowEvent e) { }
		public void windowClosed(WindowEvent e) { }
		public void windowIconified(WindowEvent e) { }
		public void windowDeiconified(WindowEvent e) { }
	}
	// -----------------------------------------
	public Visualizer() throws java.io.IOException {
		//interface for runTest
		if (vis) {   
			jf = new JFrame();
			v = new Vis();
			jf.getContentPane().add(v);
		}
		is = new InputStream[2];
		os = new OutputStream[2];
		br = new BufferedReader[2];
		tournament();
	}
	// -----------------------------------------
	public static void main(String[] args) throws java.io.IOException {
		vis = true;
		del=200; // Time between each turn in ms
		del2=3000; // Time between each match in ms
		if(debug) vis = false;
		Visualizer v = new Visualizer();
	}
	// -----------------------------------------
	void addFatalError(String message) {
		System.out.println(message);
	}
}

class ErrorReader extends Thread{
	InputStream error;
	public ErrorReader(InputStream is) {
		error = is;
	}
	public void run() {
		try {
			byte[] ch = new byte[50000];
			int read;
			while ((read = error.read(ch)) > 0) {   
				String s = new String(ch,0,read);
				System.out.print(s);
				System.out.flush();
			}
		} catch(Exception e) { }
	}
}

