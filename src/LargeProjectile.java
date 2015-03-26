
public class LargeProjectile extends FireBallSprite{
	
	protected static final double x_base_speed = 10;
	protected static final double y_base_speed = 10;
	
	protected JumperSprite jack;

	public LargeProjectile(int w, int h, ImagesLoader imsLd, JackPanel jp,
			JumperSprite j, String filename) {
		super(w, h, imsLd, jp, j, filename);
		this.jp = jp;
		jack = j;
		initPosition();
	}

	public void initPosition() {
		int h = ((int) (getPWidth() * Math.random()));
		System.out.println(h);
		setPosition(h, -50);
		setStep((int) (10), (int) (10));
	}
}
