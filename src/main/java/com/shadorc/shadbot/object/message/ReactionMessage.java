package com.shadorc.shadbot.object.message;

import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

public class ReactionMessage {

    private final GatewayDiscordClient gateway;
    private final Snowflake channelId;
    private final Collection<ReactionEmoji> reactions;

    public ReactionMessage(GatewayDiscordClient gateway, Snowflake channelId,
                           Collection<ReactionEmoji> reactions) {
        this.gateway = gateway;
        this.channelId = channelId;
        this.reactions = Collections.unmodifiableCollection(reactions);
    }

    /**
     * @param embed The embed to send.
     * @return A {@link Mono} containing a {@link Message} with {@link Reaction} added.
     */
    public Mono<Message> send(Consumer<EmbedCreateSpec> embed) {
        return this.gateway.getChannelById(this.channelId)
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(embed, channel))
                .flatMap(message -> Flux.fromIterable(this.reactions)
                        .flatMap(message::addReaction)
                        .then(Mono.just(message)));
    }

}
