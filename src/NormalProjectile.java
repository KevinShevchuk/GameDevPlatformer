
public class NormalProjectile extends FireBallSprite{

	public NormalProjectile(int w, int h, ImagesLoader imsLd, JackPanel jp,
			JumperSprite j, String filename) {
		super(w, h, imsLd, jp, j, filename);
		this.jp = jp;
		jack = j;
		initPosition();
		// TODO Auto-generated constructor stub
	}
	
	public void initPosition()
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

}
