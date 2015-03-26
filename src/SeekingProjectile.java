
public class SeekingProjectile extends FireBallSprite {

	protected static final double x_base_speed = 30;
	protected static final double y_base_speed = 30;
	
	protected JumperSprite jack;
	
	public SeekingProjectile(int w, int h, ImagesLoader imsLd, JackPanel jp,
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
		int x_start = 0, y_start = 0;
		double side = Math.round(Math.random() * 3);
		x_speed = x_base_speed; //+ getRandRange(STEP_OFFSET);
		y_speed = x_base_speed; //* Math.random();
		double x_jack = jack.getXPosn();
		double y_jack = jack.getYPosn();
		if (side == 0) {
			int h = ((int) (getPWidth() * Math.random()));
			x_start = h;
			y_start = (-50);
			setPosition(h, -50);
		}
		if (side == 1) {
			int h = ((int) (getPHeight() * Math.random()));
			x_start = (getPWidth() + 50);
			y_start = h;
			setPosition(getPWidth() + 50, h);	
		}
		if (side == 2) {
			int h = ((int) (getPHeight() * Math.random()));
			x_start = -50;
			y_start = h;
			setPosition(-50, h);
		}
		if (side == 3) {
			int h = ((int) (getPWidth() * Math.random()));
			x_start = h;
			y_start = (getPHeight() + 50);
			setPosition(h, getPHeight() + 50);
		}
		y_speed = (y_start - y_jack)/y_speed;
		x_speed = -(x_start - x_jack)/x_speed;
		y_speed = -y_speed;
		setStep((int) (x_speed), (int) (y_speed));
		// System.out.println("D: " + side + "\tStart Pos: " + getPWidth()+
		// ", " + h + "\tXS: " + x_speed + " \tYS: " + y_speed);
	}
}
