package me.shadorc.shadbot.command.image;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.image.deviantart.DeviantArtResponse;
import me.shadorc.shadbot.api.image.deviantart.Image;
import me.shadorc.shadbot.api.image.deviantart.TokenResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "image" })
public class ImageCmd extends AbstractCommand {

	private TokenResponse token;
	private long lastTokenGeneration;

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			Image image = this.getRandomPopularImage(NetUtils.encode(arg));
			if(image == null) {
				return loadingMsg.send(TextUtils.noResult(context.getArg().get())).then();
			}

			context.getAuthorAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("DeviantArt (Search: %s)", arg), image.getUrl(), avatarUrl)
							.setThumbnail("http://www.pngall.com/wp-content/uploads/2016/04/Deviantart-Logo-Transparent.png")
							.addField("Title", image.getTitle(), false)
							.addField("Author", image.getAuthor().getUsername(), false)
							.addField("Category", image.getCategoryPath(), false)
							.setImage(image.getContent().getSource()))
					.flatMap(loadingMsg::send)
					.then();

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}

		return Mono.empty();
	}

	private Image getRandomPopularImage(String encodedSearch) throws IOException {
		try {
			if(TimeUtils.getMillisUntil(lastTokenGeneration) >= TimeUnit.SECONDS.toMillis(token.getExpiresIn())) {
				this.generateAccessToken();
			}

			final URL url = new URL(String.format("https://www.deviantart.com/api/v1/oauth2/browse/popular?"
					+ "q=%s"
					+ "&timerange=alltime"
					+ "&limit=25" // The pagination limit (min: 1 max: 50)
					+ "&offset=%d" // The pagination offset (min: 0 max: 50000)
					+ "&access_token=%s",
					encodedSearch, ThreadLocalRandom.current().nextInt(150), token.getAccessToken()));

			DeviantArtResponse deviantArt = Utils.MAPPER.readValue(url, DeviantArtResponse.class);
			return deviantArt.getResults().isEmpty() ? null : Utils.randValue(deviantArt.getResults());

		} catch (IOException err) {
			return null;
		}
	}

	private synchronized void generateAccessToken() throws JSONException, IOException {
		final URL url = new URL(String.format("https://www.deviantart.com/oauth2/token?client_id=%s&client_secret=%s&grant_type=client_credentials",
				APIKeys.get(APIKey.DEVIANTART_CLIENT_ID), APIKeys.get(APIKey.DEVIANTART_API_SECRET)));
		this.token = Utils.MAPPER.readValue(url, TokenResponse.class);
		this.lastTokenGeneration = System.currentTimeMillis();
		LogUtils.infof("DeviantArt token generated: %s", token.getAccessToken());
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Search for a random image on DeviantArt.")
				.addArg("search", false)
				.setSource("https://www.deviantart.com")
				.build();
	}
}
