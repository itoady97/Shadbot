package me.shadorc.shadbot.command.game.rps;

import me.shadorc.shadbot.utils.command.Emoji;

public enum Handsign {
	ROCK("Rock", Emoji.GEM),
	PAPER("Paper", Emoji.LEAF),
	SCISSORS("Scissors", Emoji.SCISSORS);

	private final String handsign;
	private final Emoji emoji;

	Handsign(String handsign, Emoji emoji) {
		this.handsign = handsign;
		this.emoji = emoji;
	}

	public String getHandsign() {
		return handsign;
	}

	public String getRepresentation() {
		return emoji + " " + handsign;
	}

	public boolean isSuperior(Handsign other) {
		return this.equals(Handsign.ROCK) && other.equals(Handsign.SCISSORS)
				|| this.equals(Handsign.PAPER) && other.equals(Handsign.ROCK)
				|| this.equals(Handsign.SCISSORS) && other.equals(Handsign.PAPER);
	}

}