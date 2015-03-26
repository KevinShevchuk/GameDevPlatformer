// FireBallSprite.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* A fireball starts at the lower right hand side of the panel,
 and travels straight across to the left (at varying speeds).
 If it hits 'jack', it explodes (with a suitable explosion sound).

 A fireball that has left the left hand side, or exploded, is
 reused.
 */

import java.awt.*;

public abstract class FireBallSprite extends Sprite {
	// the ball's x- and y- step values are STEP +/- STEP_OFFSET
	
	protected static final int STEP_OFFSET = 3;
	protected static final double x_base_speed = 12;
	protected static final double y_base_speed = 12;
	double x_speed = 0.0;
	double y_speed = 0.0;

	protected JackPanel jp; // tell JackPanel about colliding with jack
	protected JumperSprite jack;

	public FireBallSprite(int w, int h, ImagesLoader imsLd, JackPanel jp,
			JumperSprite j, String filename) {
		super(w, h / 2, w, h, imsLd, filename);
		// the ball is positioned in the middle at the panel's rhs
		this.jp = jp;
		jack = j;
		//initPosition();
	} // end of FireBallSprite()

	public abstract void initPosition(); //allow custom initializers for each fireball type.
	
/*	private void initPosition()
	// adjust the fireball's position and its movement left
	{
		double side = Math.round(Math.random() * 3);
		x_speed = x_base_speed + getRandRange(STEP_OFFSET);
		y_speed = x_base_speed * Math.random();
		if (side == 0) {
			int h = ((int) (getPWidth() * Math.random()));
			setPosition(h, -50);
			if (Math.random() < 0.5) {
				x_speed = -x_speed;
			}
			setStep((int) x_speed, (int) y_speed);
			// System.out.println("D: " + side + "\tStart Pos: " + getPWidth()+
			// ", " + h + "\tXS: " + x_speed + " \tYS: " + y_speed);
		}
		if (side == 1) {
			int h = ((int) (getPHeight() * Math.random()));
			setPosition(getPWidth() + 50, h);
			if (Math.random() < 0.5) {
				x_speed = -x_speed;
				y_speed = -y_speed;
			} else {
				x_speed = -x_speed;
			}
			setStep((int) x_speed, (int) y_speed);
			// System.out.println("D: " + side + "\tStart Pos: " + getPWidth()+
			// ", " + h + "\tXS: " + x_speed + " \tYS: " + y_speed);
		}
		if (side == 2) {
			int h = ((int) (getPHeight() * Math.random()));
			setPosition(-50, h);
			if (Math.random() < 0.5) {
				y_speed = -y_speed;
			}
			setStep((int) x_speed, (int) y_speed);
			// System.out.println("D: " + side + "\tStart Pos: " + getPWidth()+
			// ", " + h + "\tXS: " + x_speed + " \tYS: " + y_speed);
		}
		if (side == 3) {
			int h = ((int) (getPWidth() * Math.random()));
			setPosition(h, getPHeight() + 50);
			if ((boolean) (Math.round(Math.random()) == 1)) {
				x_speed = -x_speed;
				y_speed = -y_speed;
			} else {
				y_speed = -y_speed;
			}
			setStep((int) x_speed, (int) y_speed);
			// System.out.println("D: " + side + "\tStart Pos: " + getPWidth()+
			// ", " + h + "\tXS: " + x_speed + " \tYS: " + y_speed);
		}

	} // end of initPosition()
*/
	protected int getRandRange(int x)
	// random number generator between -x and x
	{
		return ((int) (2 * x * Math.random())) - x;
	}

	public void updateSprite() {
		moveProjectiles();
		hasHitJack();
		goneOffScreen();
		super.updateSprite();
	}

	private void moveProjectiles() {
		double[] speeds = jack.adjustProjectiles(x_speed, y_speed);
		setStep((int) speeds[0], (int) speeds[1]);
	}

	private void hasHitJack()
	/*
	 * If the ball has hit jack, tell JackPanel (which will display an explosion
	 * and play a clip), and begin again.
	 */
	{
		Rectangle jackBox = jack.getMyRectangle();
		jackBox.grow(-jackBox.width / 3, 0); // make jack's bounded box thinner

		if (jackBox.intersects(getMyRectangle())) { // jack collision?
			jp.showExplosion(locx, locy + getHeight() / 2);
			// tell JackPanel, supplying it with a hit coordinate
			initPosition();
		}
	} // end of hasHitJack()

	private void goneOffScreen()
	/*
	 * when the ball has gone further than Window dimensions * 1.5 further off
	 * the screen left and right and Window height * 1.1 top and bottom, start
	 * it again.
	 */
	{
		if ((locx + getWidth()) <= (-.5 * getPWidth()) && (dx < 0)) {
			// off left and moving left
			initPosition();// start the ball in a new position
		}
		if ((locx + getWidth()) >= (1.5 * getPWidth()) && (dx > 0)) {
			// off to the right
			initPosition();
		}
		if ((locy + getHeight()) <= (-.9 * getPHeight()) && (dy < 0)) {
			// off the top
			initPosition();
		}
		if ((locy - getHeight()) >= (1.1 * getPHeight()) && (dy > 0)) {
			// off the bottom
			initPosition();
		}
	} // end of goneOffScreen()

} // end of FireBallSprite class
