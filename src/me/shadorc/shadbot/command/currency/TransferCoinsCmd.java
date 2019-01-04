package me.shadorc.shadbot.command.currency;

import java.util.ArrayList;
import java.util.List;

import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.database.DBMember;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "transfer" })
public class TransferCoinsCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		if(context.getMessage().getUserMentionIds().isEmpty()) {
			throw new MissingArgumentException();
		}

		final Snowflake senderUserId = context.getAuthorId();
		final Snowflake receiverUserId = new ArrayList<>(context.getMessage().getUserMentionIds()).get(0);
		if(receiverUserId.equals(senderUserId)) {
			throw new CommandException("You cannot transfer coins to yourself.");
		}

		final Integer coins = NumberUtils.asPositiveInt(args.get(0));
		if(coins == null) {
			throw new CommandException(
					String.format("`%s` is not a valid amount for coins.", args.get(0)));
		}

		final DBMember dbSender = Shadbot.getDatabase().getDBMember(context.getGuildId(), senderUserId);
		if(dbSender.getCoins() < coins) {
			throw new CommandException(TextUtils.NOT_ENOUGH_COINS);
		}

		final DBMember dbReceiver = Shadbot.getDatabase().getDBMember(context.getGuildId(), receiverUserId);
		if(dbReceiver.getCoins() + coins >= Config.MAX_COINS) {
			return context.getClient().getUserById(receiverUserId)
					.map(User::getUsername)
					.flatMap(username -> context.getChannel()
							.flatMap(channel -> DiscordUtils.sendMessage(String.format(
									Emoji.BANK + " (**%s**) This transfer cannot be done because %s would exceed the maximum coins cap.",
									context.getUsername(), username), channel)))
					.then();
		}

		dbSender.addCoins(-coins);
		dbReceiver.addCoins(coins);

		return Mono.zip(context.getAuthor().map(User::getMention),
				context.getClient().getUserById(senderUserId).map(User::getMention),
				context.getChannel())
				.flatMap(tuple -> DiscordUtils.sendMessage(String.format(Emoji.BANK + " %s has transfered **%s** to %s",
						tuple.getT1(), FormatUtils.coins(coins), tuple.getT2()), tuple.getT3()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Transfer coins to the mentioned user.")
				.addArg("coins", false)
				.addArg("@user", false)
				.build();
	}
}
