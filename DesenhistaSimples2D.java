/**
 * Desenha os elementos da arena na tela.
 * Fernando Bevilacqua <fernando.bevilacqua@uffs.edu.br>
 */

import java.awt.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.JFrame;

import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.Calendar;
import java.util.HashMap;

class DesenhistaSimples2D extends JFrame implements Desenhista
{
	private static final int TAM_SPRITE = 32;
	private static final long INTERVALO_RENDER_SPRITES = 150;
	private static final int FRAMES_SPRITE = 3;
	
	private static final int SPRITE_RUIVO 		= 0;
	private static final int SPRITE_MENINA 		= 1;
	private static final int SPRITE_VERDE 		= 2;
	private static final int SPRITE_AZUL 		= 3;
	private static final int SPRITE_ELFO 		= 4;
	private static final int SPRITE_CHANEL 		= 5;
	private static final int SPRITE_MAL 		= 6;
	private static final int SPRITE_MESTRE 		= 7;
	
	private Arena arena;
	private Image imgBackground;
	private Image imgSprites;
	private Image imgPontosEnergia;
	private Image imgEnergia;
	private Image imgMensagens;
	private HashMap<String, Integer> tipoSprite;	
	private int spriteAtual;
	
	public DesenhistaSimples2D() {
		final Frame a = this;
		
		// Need to add this to handle window closing events with the awt Frame.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				a.setVisible(false);
				a.dispose();
				System.exit(0);
			}
		});
		
		if(Constants.TELA_USAR_BARRA_TOPO) {
			setLocationRelativeTo(null); // centraliza a janela
			setUndecorated(false);			
		} else {
			setUndecorated(true);
		}
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(Constants.LARGURA_TELA, Constants.ALTURA_TELA + Constants.ALTURA_BARRA_TOPO_TELA);
		setVisible(true);

		createBufferStrategy(2);
	}
	
	public void init(Arena a, KeyListener k) {
		arena = a;
		tipoSprite = new HashMap<String, Integer>();
		spriteAtual = SPRITE_RUIVO;
		
		carregaAssets();
		addKeyListener(k);
	}
	
	public void render() {
		// From: http://gpwiki.org/index.php/Java:Tutorials:Double_Buffering
		// Thanks!
		
		BufferStrategy bf = this.getBufferStrategy();
		Graphics g = null;
	 
		try {
			g = bf.getDrawGraphics();
			renderiza(g);
	 
		} finally {
			// It is best to dispose() a Graphics object when done with it.
			g.dispose();
		}
	 
		// Shows the contents of the backbuffer on the screen.
		bf.show();
	 
        //Tell the System to do the Drawing now, otherwise it can take a few extra ms until 
        //Drawing is done which looks very jerky
        Toolkit.getDefaultToolkit().sync();
	}

	private void renderiza(Graphics g) {
		desenhaBackground(g);
		
		for(Entidade e : arena.getEntidades()) {
			if(e instanceof Agente) {
				desenhaAgente(g, (Agente) e);
				
			} else if(e instanceof PontoEnergia) {
				desenhaPontoEnergia(g, (PontoEnergia) e);
			}
		}
	}
	
	private void carregaAssets() {
		try {
			imgBackground 		= ImageIO.read(new File("imagens/desert.png"));			
			imgSprites	 		= ImageIO.read(new File("imagens/sprites1.png"));
			imgPontosEnergia	= ImageIO.read(new File("imagens/towers.png"));
			imgEnergia			= ImageIO.read(new File("imagens/energy.png"));
			imgMensagens		= ImageIO.read(new File("imagens/messages.png"));
			
		} catch(IOException e) {
			System.out.println("Não foi possível carregar a imagem...");
			System.exit(0);
		}	
	}
	
	private void desenhaSprite(Graphics g, int x, int y, int i, int j, int tipoSprite) {
		int linha  = (tipoSprite <= 3 ? 0 : 4) + i;
		int coluna = j + (tipoSprite % 4) * FRAMES_SPRITE;
		
		g.drawImage(imgSprites, x, y, x + TAM_SPRITE, y + TAM_SPRITE, coluna * TAM_SPRITE, linha* TAM_SPRITE, coluna * TAM_SPRITE + TAM_SPRITE, linha * TAM_SPRITE+TAM_SPRITE, null);
	}
	
	private void desenhaSprite(Graphics g, Image img, int x, int y, int frame, int largura, int altura) {
		g.drawImage(img, x, y, x + largura, y + altura, frame * altura, /*frame * largura*/ 0, frame * altura + altura, /*linha * largura +*/ largura, null);
	}
	
	private void desenhaBackground(Graphics g) {
		int linhas, colunas, i, j, largura, altura;
		
		largura = imgBackground.getWidth(this);
		altura	= imgBackground.getHeight(this);

		colunas = (int) Math.ceil((double)Constants.LARGURA_TELA / largura);
		linhas 	= (int) Math.ceil((double)Constants.ALTURA_TELA / altura);
		
		for(i = 0; i < linhas; i++) {
			for(j = 0; j < colunas; j++) {
				g.drawImage(imgBackground, j*largura, i*altura, j*largura+largura, i*altura+altura, 0, 0, largura, altura - 20, null);
			}
		}
	}
	
	private void desenhaAnimacao(String anim, Image img, Graphics g, Agente a, int largura, int altura) {
		int frameAtual;
		long tempoAgora = System.currentTimeMillis();
		
		if(isAnimacaoAtiva(anim, a, tempoAgora)) {
			frameAtual = getFrameCorrente(anim, a);
			
			if(tempoAgora >= getTimeProximoRender(anim, a)) {			
				setTimeProximoRender(anim, a, tempoAgora + INTERVALO_RENDER_SPRITES);
				// TODO: corrigir o número máximo de frames
				incrementaFrame(anim, a, frameAtual);
			}
			
			desenhaSprite(g, img, a.getX() - largura/2, a.getY() - altura/2, frameAtual, largura, altura);
		}			
	}
	
	private void desenhaAgente(Graphics g, Agente a) {
		int i 			= 0;
		int frameAtual 	= getFrameCorrente("agente", a);
		long tempoAgora = System.currentTimeMillis();

		desenhaAnimacao("energia", 		imgEnergia,   g, a, 200, 200);
		desenhaAnimacao("recebeuMsg", 	imgMensagens, g, a, 160, 160);
		desenhaAnimacao("enviouMsg", 	imgMensagens, g, a, 160, 160);
		
		switch(a.getDirecao()) {
			case Agente.DIREITA:
				i = 2;
				break;
				
			case Agente.ESQUERDA:
				i = 1;
				break;
				
			case Agente.CIMA:
				i = 3;
				break;
				
			case Agente.BAIXO:	
				i = 0;
				break;
				
			default:
			// idle?
		}
		
		frameAtual = getFrameCorrente("agente", a);
		
		if(tempoAgora >= getTimeProximoRender("agente", a)) {			
			setTimeProximoRender("agente", a, tempoAgora + INTERVALO_RENDER_SPRITES);
			incrementaFrame("agente", a, frameAtual);
		}

		desenhaSprite(g, a.getX(), a.getY(), i, frameAtual, getTipoSprite(a));
	}
	
	private boolean isAnimacaoAtiva(String anim, Agente a, long tempoAgora) {
		Long tempo = (Long)a.getDados().get(anim);
		return tempo != null && tempo >= tempoAgora;
	}
	
	private void ativaAnimacao(String anim, Agente a, long duracao) {
		a.getDados().put(anim, System.currentTimeMillis() + duracao);	
	}
	
	private Long getTimeProximoRender(String anim, Agente a) {
		Long tempo = (Long)a.getDados().get(anim + "proximoRender");
		return tempo != null ? tempo : 0;
	}
	
	private void setTimeProximoRender(String anim, Agente a, long tempo) {
		a.getDados().put(anim + "proximoRender", tempo);
	}
	
	private Integer getFrameCorrente(String anim, Agente a) {
		Integer frame = (Integer)a.getDados().get(anim + "frame");
		return frame != null ? frame : 0;
	}
	
	private void incrementaFrame(String anim, Agente a, int frameAtual) {
		a.getDados().put(anim + "frame", frameAtual < (FRAMES_SPRITE -1) ? frameAtual + 1 : 0);
	}
	
	private Integer getTipoSprite(Agente a) {
		Integer tipo = (Integer)a.getDados().get("sprite");
		
		if(tipo == null) {
			if(tipoSprite.get(a.getEquipe()) == null) {
				tipo = spriteAtual++;
				tipoSprite.put(a.getEquipe(), tipo);
			} else {
				tipo = (Integer)tipoSprite.get(a.getEquipe());
			}
		}
		
		return tipo;
	}
	
	private void desenhaPontoEnergia(Graphics g, PontoEnergia p) {
		g.drawImage(imgPontosEnergia, p.getX(), p.getY(), p.getX()+50, p.getY()+60, 55, 0, 90, 60, null);
	}
	
	
	//////////////////////
	// Métodos invocados pela arena para avisar sobre acontecimentos importantes
	/////////////////////
	
	public void agenteRecebeuEnergia(Agente a) {
		//ativaAnimacao("energia", a, 500);
	}
	
	public void agenteTomouDano(Agente a) {
		
	}

	public void agenteBateuAlguem(Agente batendo, Agente apanhando) {
		
	}

	public void agenteGanhouCombate(Agente a) {
		
	}

	public void agenteMorreu(Agente a) {
		
	}
	
	public void agenteMorreuPorExcecao(Agente a, Exception e) {
		
	}

	public void agenteClonou(Agente origem, Agente clone) {
	}

	public void agenteEnviouMensagem(Agente a, String msg) {
		ativaAnimacao("enviouMsg", a, 1000);	
	}

	public void agenteRecebeuMensagem(Agente destinatario, Agente remetente) {
		ativaAnimacao("recebeuMsg", destinatario, 500);
	}
}
