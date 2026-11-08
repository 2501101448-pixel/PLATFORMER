package Main;

import javax.swing.JFrame;

public class GameWindow {

	private JFrame jframe;

	public GameWindow(GamePanel gamePanel, String stageName) {

		jframe = new JFrame();
		jframe.setTitle("Platformer - " + stageName);
		
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.add(gamePanel);
		jframe.setLocationRelativeTo(null);
		jframe.setResizable(false);
		jframe.pack();
		jframe.setVisible(true);

	}
}
