package bkampfbot.modes;

/*
 Copyright (C) 2011  georf@georf.de

 This file is part of BKampfBot.

 BKampfBot is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 2 of the License, or
 any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import bkampfbot.Control;
import bkampfbot.Utils;
import bkampfbot.output.Output;
import bkampfbot.output.SimpleFile;
import bkampfbot.plan.PlanObject;

public final class Jagd extends PlanObject {

	private static final String[] names = { "Film", "Sprichwort", "Musik",
			"Games", "Sport" };

	private static Jagd mode = null;

	private int wordsToRun = -1;

	private int letterMin = 0;
	private int letterMax = 0;
	private int percent = 100;
	private boolean luck = false;

	private int wordsSolved = 0;

	public Jagd(JSONObject setup) {
		super("Wörterjagd");

		try {
			letterMin = setup.getInt("min");
		} catch (JSONException e) {
		}
		try {
			letterMax = setup.getInt("max");
		} catch (JSONException e) {
		}
		try {
			percent = setup.getInt("Prozent");
		} catch (JSONException e) {
		}
		try {
			luck = setup.getBoolean("Versuchen");
		} catch (JSONException e) {
		}

		if (percent > 100) {
			percent = 100;
		}

		if (percent < 0) {
			percent = 0;
		}

	}

	public static Jagd getInstance() {
		return getInstance(-1);
	}

	public static void setInstance(Jagd j) {
		mode = j;
	}

	public static Jagd getInstance(int wordsToRun) {
		if (mode == null) {
			mode = new Jagd(new JSONObject());
		}

		mode.wordsToRun = wordsToRun;

		return mode;
	}

	public void run() {
		Output.printClockLn("Wörterjagd", 1);

		int availablePoints;
		int score;
		try {
			do {
				JSONObject result = Utils.getJSON("games/getWheelData");
				JSONArray data = result.getJSONArray("data");

				availablePoints = result.getJSONArray("credit")
						.getJSONObject(0).getInt("highscore");
				score = result.getJSONArray("credit").getJSONObject(0).getInt(
						"sloganhighscore");

				Output.printTabLn("Aktuelle Punktzahl: " + availablePoints, 2);

				if (data.length() != 5) {
					Output
							.println("Etwas stimmt nicht. Abbruch.",
									Output.ERROR);
				}

				for (int i = 0; i < 5 && availablePoints > 20
						&& score < percent && doMore(); i++) {
					JSONObject current = data.getJSONObject(i);

					Output.printTab(names[i] + ": ", Output.DEBUG);

					if (current.getInt("result") == 1) {

						Output.println(" erledigt", Output.DEBUG);
						continue;

					}

					Output.println(" zu lösen", Output.DEBUG);

					if (letterMax > 0) {
						ArrayList<Character> alpha = new ArrayList<Character>();
						for (int j = 65; j < 65 + 26; j++) {
							alpha.add((char) j);
						}

						// check for letters
						for (int z = 0; z < letterMax; z++) {
							if (availablePoints < current.getInt("costletter")) {
								return;
							}

							if (z >= letterMin && Math.random() > 0.4) {
								continue;
							}
							int currentAlpha = (new Random()).nextInt(alpha
									.size() - 1);
							char currentLetter = alpha.get(currentAlpha);
							alpha.remove(currentAlpha);

							Output.printTabLn("Kaufe ein " + currentLetter,
									Output.INFO);

							List<NameValuePair> nvps = new ArrayList<NameValuePair>();
							nvps.add(new BasicNameValuePair("buyletter",
									currentLetter + ""));
							nvps.add(new BasicNameValuePair("type", "letter"));
							Utils.getString("games/buyWheel", nvps);

							availablePoints -= current.getInt("costletter");

							Control.sleep(45);
						}
					}

					String md5;
					try {

						String dec = decrypt(current.getJSONArray("slogan"));

						Output.printTab(dec + ": ", Output.INFO);

						SimpleFile.append("Jagd." + names[i] + ".txt", dec
								+ "\n");

						MessageDigest m = MessageDigest.getInstance("MD5");
						byte[] data1 = ("OsShlljuozll11T670OO00OO" + dec
								.replaceAll("\\s", "")).getBytes();
						m.update(data1, 0, data1.length);
						BigInteger in = new BigInteger(1, m.digest());
						md5 = String.format("%1$032X", in);
					} catch (NoSuchAlgorithmException e) {
						Output.error(e);
						return;
					}

					md5 = md5.toLowerCase();

					List<NameValuePair> nvpn = new ArrayList<NameValuePair>();
					nvpn.add(new BasicNameValuePair("type", "slogan"));
					nvpn.add(new BasicNameValuePair("secret", md5));

					Control.sleep(new Random().nextInt(100) + 100);

					String res = Utils.getString("games/buyWheel", nvpn);

					if (res.equals("errorStatus=ok&right=right")) {
						Output.println("richtig", Output.INFO);
						score++;
						wordsSolved++;
					} else {
						Output.println("falsch", Output.INFO);
						break;
					}

					availablePoints -= 20;

				}

				// Auf gut Glück -klicken
				if (score >= percent && luck) {
					int level = 0;

					if (score >= 100) {
						level = 100;
					} else if (score >= 75) {
						level = 75;
					} else if (score >= 50) {
						level = 50;
					} else if (score >= 20) {
						level = 20;
					} else if (score >= 10) {
						level = 10;
					}

					if (level > 0) {
						Control.sleep(40, Output.INFO);

						// errorStatus=ok&modus=item&item_pic=wurf387.png&item_type=ring&wintext=Teekessel+mit+ohrenbet%C3%A4ubender+Pfeife
						String line = Utils.getString("games/payout");
						Map<String, String> para = Utils.getUrlParameters(line);

						if (para.get("modus").equals("item")) {
							Output.printClockLn("Gewinn: "
									+ para.get("item_type") + ", "
									+ para.get("wintext"), Output.INFO);
						} else if (para.get("modus").equals("trost")) {
							Output.printClockLn("Gewinn: "
									+ para.get("wintext") + " "
									+ para.get("preis"), Output.INFO);
						} else {
							Output.println(para.toString(), Output.INFO);
						}

						score -= level;
					}
				}

			} while (availablePoints > 20 && score < percent && doMore());

		} catch (JSONException e) {
			Output.error(e);
			return;
		}
	}

	private boolean doMore() {
		return (wordsToRun == -1) || (wordsSolved < wordsToRun);
	}

	public int getWordsSolved() {
		return wordsSolved;
	}

	private final String decrypt(final JSONArray slogan) throws JSONException {
		final char[] letters = new char[36];

		letters[6] = 'A';
		letters[12] = 'B';
		letters[15] = 'C';
		letters[5] = 'D';
		letters[34] = 'E';
		letters[16] = 'F';
		letters[8] = 'G';
		letters[23] = 'H';
		letters[9] = 'I';
		letters[20] = 'J';
		letters[33] = 'K';
		letters[22] = 'L';
		letters[17] = 'M';
		letters[28] = 'N';
		letters[19] = 'O';
		letters[30] = 'P';
		letters[7] = 'Q';
		letters[4] = 'R';
		letters[10] = 'S';
		letters[11] = 'T';
		letters[27] = 'U';
		letters[13] = 'V';
		letters[31] = 'W';
		letters[14] = 'X';
		letters[29] = 'Y';
		letters[35] = 'Z';
		letters[18] = 'ß';
		letters[32] = 'O';
		letters[24] = 'U';
		letters[25] = 'A';

		String encrypted = "";

		for (int i = 0; i < slogan.length(); i++) {
			final JSONArray word = slogan.getJSONArray(i);
			String encryptedWord = "";

			for (int j = 0; j < word.length(); j++) {
				final int wordPart = word.getInt(j);
				final String decodedPart = String.valueOf(wordPart / 3 - 26);

				String loc7 = "";
				for (int number = 0; number < decodedPart.length(); number++) {
					if ((decodedPart.substring(number, number + 1).equals("1")
							|| decodedPart.substring(number, number + 1)
									.equals("2") || decodedPart.substring(
							number, number + 1).equals("3"))
							&& loc7.equals("")) {
						loc7 = decodedPart.substring(number, number + 1);
					} else if (!loc7.equals("")) {
						encryptedWord += letters[Integer
								.valueOf((loc7 + decodedPart.substring(number,
										number + 1)))];
						loc7 = "";
					} else {
						encryptedWord += letters[Integer.valueOf(decodedPart
								.substring(number, number + 1))];
					}
				}

			}

			if (!encrypted.equals("")) {
				encrypted += " " + encryptedWord;
			} else {
				encrypted = encryptedWord;
			}

		}
		return encrypted;
	}
}