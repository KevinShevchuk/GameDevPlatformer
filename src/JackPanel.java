// JackPanel.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* The game's drawing surface. Uses active rendering to a JPanel
 with the help of Java 3D's timer.

 Set up the background and sprites, and update and draw
 them every period nanosecs.

 The background is a series of ribbons (wraparound images
 that move), and a bricks ribbon which the JumpingSprite
 (called 'jack') runs and jumps along.

 'Jack' doesn't actually move horizontally, but the movement
 of the background gives the illusion that it is.

 There is a fireball sprite which tries to hit jack. It shoots
 out horizontally from the right hand edge of the panel. After
 MAX_HITS hits, the game is over. Each hit is accompanied 
 by an animated explosion and sound effect.

 The game begins with a simple introductory screen, which
 doubles as a help window during the course of play. When
 the help is shown, the game pauses.

 The game is controlled only from the keyboard, no mouse
 events are caught.
 */

import javax.swing.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.*;

public class JackPanel extends JPanel implements Runnable, ImagesPlayerWatcher {
	private static final int PWIDTH = 1024; // size of panel
	private static final int PHEIGHT = 768;

	private static final int NO_DELAYS_PER_YIELD = 16;
	/*
	 * Number of frames with a delay of 0 ms before the animation thread yields
	 * to other running threads.
	 */
	private static final int MAX_FRAME_SKIPS = 5;
	// no. of frames that can be skipped in any one animation loop
	// i.e the games state is updated but not rendered

	// image, bricks map, clips loader information files
	private static final String IMS_INFO = "imsInfo.txt";
	private static final String BRICKS_INFO = "bricksInfo.txt";
	private static final String CLOUDS_INFO = "cloudsInfo.txt";
	private static final String SNDS_FILE = "clipsInfo.txt";

	// names of the explosion clips
	private static final String[] exploNames = { "explo1", "explo2", "explo3" };

	private static final int GoalDistance = 300;
	// number of times jack can be hit by a fireball before the game is over

	private Thread animator; // the thread that performs the animation
	private volatile boolean running = false; // used to stop the animation
												// thread
	private volatile boolean isPaused = false;

	private long period; // period between drawing in _nanosecs_

	private JumpingJack jackTop;
	private ClipsLoader clipsLoader;

	private JumperSprite jack; // the sprites
	private FireBallSprite fireball1, fireball2, fireball3, seeker, largeRock;
	private RibbonsManager ribsMan; // the ribbons manager
	private BricksManager bricksMan; // the bricks manager
	private BricksManager cloudsMan; // the manager for foreground clouds.

	private long gameStartTime; // when the game started
	private int timeSpentInGame;

	// used at game termination
	private volatile boolean gameOver = false;
	private volatile boolean victory = false;
	private double stam = 30.0;
	private int score;
	private int distance = 0;

	// for displaying messages
	private Font msgsFont;
	private FontMetrics metrics;

	// off-screen rendering
	private Graphics dbg;
	private Image dbImage = null;

	// to display the title/help screen
	private boolean showHelp;
	private BufferedImage helpIm;

	// explosion-related
	private ImagesPlayer explosionPlayer = null;
	private boolean showExplosion = false;
	private int explWidth, explHeight; // image dimensions
	private int xExpl, yExpl; // coords where image is drawn

	private int numHits = 0; // the number of times 'jack' has been hit

	private boolean right, up, left, down;

	public JackPanel(JumpingJack jj, long period) {
		jackTop = jj;
		this.period = period;

		setDoubleBuffered(false);
		setBackground(Color.white);
		setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

		setFocusable(true);
		requestFocus(); // the JPanel now has focus, so receives key events

		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				processKey(e);
			}

			public void keyReleased(KeyEvent e) {
				processRelease(e);
			}
		});

		// initialise the loaders
		ImagesLoader imsLoader = new ImagesLoader(IMS_INFO);
		clipsLoader = new ClipsLoader(SNDS_FILE);

		// initialise the game entities
		bricksMan = new BricksManager(PWIDTH, PHEIGHT, BRICKS_INFO, imsLoader);
		int brickMoveSize = bricksMan.getMoveSize();
		cloudsMan = new BricksManager(PWIDTH, PHEIGHT, CLOUDS_INFO, imsLoader);

		ribsMan = new RibbonsManager(PWIDTH, PHEIGHT, brickMoveSize, imsLoader);

		jack = new JumperSprite(PWIDTH, PHEIGHT, brickMoveSize, bricksMan,
				imsLoader, (int) (period / 1000000L)); // in ms

		fireball1 = new NormalProjectile(PWIDTH, PHEIGHT, imsLoader, this, jack, "rock_normal");
		fireball2 = new NormalProjectile(PWIDTH, PHEIGHT, imsLoader, this, jack, "rock_normal");
		fireball3 = new NormalProjectile(PWIDTH, PHEIGHT, imsLoader, this, jack, "rock_normal");
		largeRock = new LargeProjectile(PWIDTH, PHEIGHT, imsLoader, this, jack, "rock_big");
		seeker = new SeekingProjectile(PWIDTH, PHEIGHT, imsLoader, this, jack, "rock_tiny");

		// prepare the explosion animation
		explosionPlayer = new ImagesPlayer("rock_break",
				(int) (period / 1000000L), 0.5, false, imsLoader);
		BufferedImage explosionIm = imsLoader.getImage("rock_break");
		explWidth = explosionIm.getWidth();
		explHeight = explosionIm.getHeight();
		explosionPlayer.setWatcher(this); // report animation's end back here

		// prepare title/help screen
		helpIm = imsLoader.getImage("splash");
		showHelp = true; // show at start-up
		isPaused = true;

		// set up message font
		msgsFont = new Font("SansSerif", Font.BOLD, 24);
		metrics = this.getFontMetrics(msgsFont);
		
		right = false;
		up = false;
		left = false; 
		down = false;

	} // end of JackPanel()

	private void processKey(KeyEvent e)
	// handles termination, help, and game-play keys
	{
		int keyCode = e.getKeyCode();

		// termination keys
		// listen for esc, q, end, ctrl-c on the canvas to
		// allow a convenient exit from the full screen configuration
		if ((keyCode == KeyEvent.VK_ESCAPE) || (keyCode == KeyEvent.VK_Q)
				|| (keyCode == KeyEvent.VK_END)
				|| ((keyCode == KeyEvent.VK_C) && e.isControlDown()))
			running = false;

		// help controls
		if (keyCode == KeyEvent.VK_H) {
			if (showHelp) { // help being shown
				showHelp = false; // switch off
				isPaused = false;
			} else { // help not being shown
				showHelp = true; // show it
				isPaused = true; // isPaused may already be true
			}
		}

		// game-play keys
		if (!isPaused && !gameOver) {			
			// move the sprite and ribbons based on the arrow key pressed
			if (keyCode == KeyEvent.VK_LEFT) {
				if (!left) {
					left = true;
					if (right){
						right = false;
					}
					jack.setMovingLeft(true);
					jack.moveLeft();
					bricksMan.moveRight(); // bricks and ribbons move the other way
					cloudsMan.moveRight();
					ribsMan.moveRight();
				}
			} if (keyCode == KeyEvent.VK_RIGHT) {
				if (!right) {
					right = true;
					if (left){
						left = false;
					}
					jack.setMovingRight(true);
					jack.moveRight();
					bricksMan.moveLeft();
					cloudsMan.moveLeft();
					ribsMan.moveLeft();
				}
			} if (keyCode == KeyEvent.VK_UP) {
				if (!up) {
					up = true;
					if (down){
						down = false;
					}
					jack.moveUp();
					bricksMan.stayStill();
					ribsMan.stayStill();
					cloudsMan.stayStill();
				}
			} if (keyCode == KeyEvent.VK_DOWN) {
				if (!down) {
					down = true;
					if (up) {
						up = false;
					}
					jack.moveDown();
					bricksMan.stayStill();
					ribsMan.stayStill();
					cloudsMan.stayStill();
				}
			} if (keyCode == KeyEvent.VK_SPACE) {
				jack.jump(); // jumping has no effect on the bricks/ribbons
			/*} if (keyCode == KeyEvent.VK_SHIFT) {
				if (shift) {
					if (stam > 0) {
						stam = stam - 3;
					}
					else
					{
						shift = false;
						jack.stopDash();
					}	
				}
				if (!shift) {
					shift = true;
					jack.dash();
				}*/
			}
		}
	} // end of processKey()

	private void processRelease(KeyEvent e) {
		// TODO Auto-generated method stub
		int keyCode = e.getKeyCode();
		if (!isPaused && !gameOver) {
			// move the sprite and ribbons based on the arrow key pressed
			if (keyCode == KeyEvent.VK_LEFT) {
				left = false;
				jack.setMovingLeft(false);
				jack.stayStill();
				bricksMan.stayStill();
				cloudsMan.stayStill();
				ribsMan.stayStill();
			} if (keyCode == KeyEvent.VK_RIGHT) {
				right = false;
				jack.setMovingRight(false);
				jack.stayStill();
				bricksMan.stayStill();
				cloudsMan.stayStill();
				ribsMan.stayStill();
			} if (keyCode == KeyEvent.VK_UP) {
				if (up){
					up = false;
					jack.stayStill();
				}
				bricksMan.stayStill();
				cloudsMan.stayStill();
				ribsMan.stayStill();
			} if (keyCode == KeyEvent.VK_DOWN) {
				if (down){
					down = false;
					jack.stayStill();
				}
				bricksMan.stayStill();
				cloudsMan.stayStill();
				ribsMan.stayStill();
			} 
			/*if (keyCode == KeyEvent.VK_SHIFT) {
				if(shift) {
					shift = false;
					jack.stopDash();
				}
			}*/
		}
	}

	public void showExplosion(int x, int y)
	// called by fireball sprite when it hits jack at (x,y)
	{
		if (!showExplosion) { // only allow a single explosion at a time
			showExplosion = true;
			xExpl = x - explWidth / 2; // \ (x,y) is the center of the explosion
			yExpl = y - explHeight / 2;

			/*
			 * Play an explosion clip, but cycle through them. This adds
			 * variety, and gets round not being able to play multiple instances
			 * of a clip at the same time.
			 */
			clipsLoader.play(exploNames[numHits % exploNames.length], false);
			numHits++;
		}
	} // end of showExplosion()

	public void sequenceEnded(String imageName)
	// called by ImagesPlayer when the explosion animation finishes
	{
		showExplosion = false;
		explosionPlayer.restartAt(0); // reset animation for next time
		numHits ++;
			//gameOver = true;
			//score = (int) ((System.nanoTime() - gameStartTime) / 1000000000L);
			//clipsLoader.play("applause", false);

	} // end of sequenceEnded()
	
	public void hasFallenOffMap(){
		if (jack.getYPosn() > 768)
		{
			gameOver = true;
		}
	}
	
	public void checkForVictory() {
		distance = jack.fetchDistance();
		if (Math.abs(distance) > GoalDistance) {
			victoryMessage(dbg);
		}
	}

	public void addNotify()
	// wait for the JPanel to be added to the JFrame before starting
	{
		super.addNotify(); // creates the peer
		startGame(); // start the thread
	}

	private void startGame()
	// initialise and start the thread
	{
		if (animator == null || !running) {
			animator = new Thread(this);
			animator.start();
		}
	} // end of startGame()

	// ------------- game life cycle methods ------------
	// called by the JFrame's window listener methods

	public void resumeGame()
	// called when the JFrame is activated / deiconified
	{
		if (!showHelp) // CHANGED
			isPaused = false;
	}

	public void pauseGame()
	// called when the JFrame is deactivated / iconified
	{
		isPaused = true;
	}

	public void stopGame()
	// called when the JFrame is closing
	{
		running = false;
	}

	// ----------------------------------------------

	public void run()
	/* The frames of the animation are drawn inside the while loop. */
	{
		long beforeTime, afterTime, timeDiff, sleepTime;
		long overSleepTime = 0L;
		int noDelays = 0;
		long excess = 0L;

		gameStartTime = System.nanoTime();
		beforeTime = gameStartTime;

		running = true;

		while (running) {
			gameUpdate();
			gameRender();
			paintScreen();

			afterTime = System.nanoTime();
			timeDiff = afterTime - beforeTime;
			sleepTime = (period - timeDiff) - overSleepTime;

			if (sleepTime > 0) { // some time left in this cycle
				try {
					Thread.sleep(sleepTime / 1000000L); // nano -> ms
				} catch (InterruptedException ex) {
				}
				overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
			} else { // sleepTime <= 0; the frame took longer than the period
				excess -= sleepTime; // store excess time value
				overSleepTime = 0L;

				if (++noDelays >= NO_DELAYS_PER_YIELD) {
					Thread.yield(); // give another thread a chance to run
					noDelays = 0;
				}
			}

			beforeTime = System.nanoTime();

			/*
			 * If frame animation is taking too long, update the game state
			 * without rendering it, to get the updates/sec nearer to the
			 * required FPS.
			 */
			int skips = 0;
			while ((excess > period) && (skips < MAX_FRAME_SKIPS)) {
				excess -= period;
				gameUpdate(); // update state but don't render
				skips++;
			}
		}
		System.exit(0); // so window disappears
	} // end of run()

	private void gameUpdate() {
		if (!isPaused && !gameOver) {
			if (jack.willHitBrick()) { // collision checking first
				jack.stayStill(); // stop jack and scenery
				bricksMan.stayStill();
				cloudsMan.stayStill();
				ribsMan.stayStill();
			}
			ribsMan.update(); // update background and sprites
			bricksMan.update();
			cloudsMan.update();
			jack.updateSprite();
			fireball1.updateSprite();
			fireball2.updateSprite();
			fireball3.updateSprite();
			largeRock.updateSprite();
			seeker.updateSprite();
			checkStamina();
			

			if (showExplosion)
				explosionPlayer.updateTick(); // update the animation
		}
	} // end of gameUpdate()

	private void gameRender() {
		if (dbImage == null) {
			dbImage = createImage(PWIDTH, PHEIGHT);
			if (dbImage == null) {
				System.out.println("dbImage is null");
				return;
			} else
				dbg = dbImage.getGraphics();
		}

		// draw a white background
		dbg.setColor(Color.white);
		dbg.fillRect(0, 0, PWIDTH, PHEIGHT);

		// draw the game elements: order is important
		ribsMan.display(dbg); // the background ribbons
		bricksMan.display(dbg); // the bricks
		cloudsMan.display(dbg);
		jack.drawSprite(dbg); // the sprites
		fireball1.drawSprite(dbg);
		fireball2.drawSprite(dbg);
		fireball3.drawSprite(dbg);
		seeker.drawSprite(dbg);
		largeRock.drawSprite(dbg);
		
		distance = jack.fetchDistance();

		if (showExplosion) // draw the explosion (in front of jack)
			dbg.drawImage(explosionPlayer.getCurrentImage(), xExpl, yExpl, null);

		reportStats(dbg);
		checkStamina();
		hasFallenOffMap();
		checkForVictory();
		if (gameOver)
			gameOverMessage(dbg);

		if (showHelp) // draw the help at the very front (if switched on)
			dbg.drawImage(helpIm, (PWIDTH - helpIm.getWidth()) / 2,
					(PHEIGHT - helpIm.getHeight()) / 2, null);
	} // end of gameRender()
	
	private void checkStamina() {
		if (jack.getFlyingStatus() == false) {
			if (stam < 30) {
				stam = stam + .1;
			}
		}
		else {
			stam = stam - .08;
			if (stam < 0) {
				stam = 0;
				jack.jump();
			}
		}	
	}

	private void reportStats(Graphics g)
	// Report the number of hits, and time spent playing
	{
		if (!gameOver) // stop incrementing the timer once the game is over
			timeSpentInGame = (int) ((System.nanoTime() - gameStartTime) / 1000000000L); // ns
																							// -->																							// secs
		g.setColor(Color.red);
		g.setFont(msgsFont);
		g.drawString("Hits: " + numHits, 15, 25);
		g.drawString("Time: " + timeSpentInGame + " secs", 15, 50);
		
		g.setColor(Color.ORANGE);
		String msg = "Goal: " + GoalDistance + " meters";
		int x = (PWIDTH - metrics.stringWidth(msg)) / 2;
		g.setFont(msgsFont);
		g.drawString(msg, x, 25);
		
		g.setColor(Color.GREEN);
		String msg1 = "Traveled: " + Math.abs(distance);
		int x1 = (PWIDTH - metrics.stringWidth(msg1)) / 2;
		g.setFont(msgsFont);
		g.drawString(msg1, x, 50);
		
		if (jack.getFlyingStatus() == false) {
			g.setColor(Color.blue);
		}
		else {
			g.setColor(Color.red);
		}
		g.drawString("Stamina: " + (int) stam , 15, 740);
		g.setColor(Color.black);
	} // end of reportStats()

	private void gameOverMessage(Graphics g)
	// Center the game-over message in the panel.
	{
		String msg = "Game Over. Your score: 0";

		int x = (PWIDTH - metrics.stringWidth(msg)) / 2;
		int y = (PHEIGHT - metrics.getHeight()) / 2;
		g.setColor(Color.black);
		g.setFont(msgsFont);
		g.drawString(msg, x, y);
		isPaused = true;
	} // end of gameOverMessage()
	
	private void victoryMessage(Graphics g)
	// Center the game-over message in the panel.
	{
		if(!victory) {
			victory = true;
			
			int time = ((int) ((System.nanoTime() - gameStartTime) / 1000000000L));
			if (numHits == 0) {
				score = (int) (1000/time);
			}
			else {
				score = (int) (1000/(time / numHits));
			}
		}
			String msg = "You Escaped! Your score: " + score;
	
			int x = (PWIDTH - metrics.stringWidth(msg)) / 2;
			int y = (PHEIGHT - metrics.getHeight()) / 2;
			g.setColor(Color.black);
			g.setFont(msgsFont);
			g.drawString(msg, x, y);
			isPaused = true;
	} // end of gameOverMessage()

	private void paintScreen()
	// use active rendering to put the buffered image on-screen
	{
		Graphics g;
		try {
			g = this.getGraphics();
			if ((g != null) && (dbImage != null))
				g.drawImage(dbImage, 0, 0, null);
			// Sync the display on some systems.
			// (on Linux, this fixes event queue problems)
			Toolkit.getDefaultToolkit().sync();
			g.dispose();
		} catch (Exception e) {
			System.out.println("Graphics context error: " + e);
		}
	} // end of paintScreen()

} // end of JackPanel class
