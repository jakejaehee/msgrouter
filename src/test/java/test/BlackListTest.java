package test;

import java.util.Date;

import msgrouter.engine.socket.server.BlackList;
import msgrouter.engine.socket.server.Score;

public class BlackListTest {
	public static void main(String[] args) {
		BlackList bl = new BlackList(10000L, 3000, -5);

		for (int i = 0; i < 10000; i++) {
			Score score = bl.getScore("localhost");
			if (score.white) {
				if (System.currentTimeMillis() - score.lastTime < bl
						.getWhiteInterval()) {
					score.value--;
				}
				if (score.value <= bl.getBlackScore()) {
					score.white = false;
				}
			}
			score.lastTime = System.currentTimeMillis();
			System.out.println(new Date() + " ### " + score);
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
			}
			if (!score.white) {
				System.out.println(new Date() + " ### loop ends");
				break;
			}
		}
	}
}
