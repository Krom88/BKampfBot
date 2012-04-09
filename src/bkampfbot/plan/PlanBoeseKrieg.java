package bkampfbot.plan;

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

import json.JSONObject;
import bkampfbot.exceptions.FatalError;
import bkampfbot.state.Opponent;

public class PlanBoeseKrieg extends PlanBoese {
	private static OpponentList list;
	private static Opponent last;

	public PlanBoeseKrieg(JSONObject object) throws FatalError {
		super(object, "BoeseKrieg");

		if (PlanBoeseKrieg.list == null) {
			PlanBoeseKrieg.list = new OpponentList();
		}
	}

	@Override
	protected OpponentList getOpponentList() {
		return PlanBoeseKrieg.list;
	}

	public static void initiate() {
		PlanBoeseKrieg.list = null;
	}

	@Override
	protected final String getEnemyListUri() {
		return "/1/war";
	}

	@Override
	protected Opponent getOpponent() {
		return last;
	}

	@Override
	protected void setOpponent(Opponent l) {
		last = l;
	}

}
