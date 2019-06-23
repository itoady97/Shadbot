package me.shadorc.shadbot.object.help;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HelpBuilder {

    private final Context context;
    private final BaseCmd cmd;
    private final List<Argument> args;
    private final List<EmbedFieldEntity> fields;

    @Nullable
    private String thumbnail;
    @Nullable
    private String description;
    @Nullable
    private String usage;
    @Nullable
    private String example;
    @Nullable
    private String source;
    private String delimiter;

    public HelpBuilder(BaseCmd cmd, Context context) {
        this.context = context;
        this.cmd = cmd;
        this.args = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.delimiter = Config.DEFAULT_COMMAND_DELIMITER;
    }

    public HelpBuilder setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
        return this;
    }

    public HelpBuilder setDescription(String description) {
        this.description = String.format("**%s**", description);
        return this;
    }

    public HelpBuilder setFullUsage(String usage) {
        this.usage = usage;
        return this;
    }

    public HelpBuilder setUsage(String usage) {
        return this.setFullUsage(String.format("%s%s %s", this.context.getPrefix(), this.cmd.getName(), usage));
    }

    public HelpBuilder setExample(String example) {
        this.example = example;
        return this;
    }

    public HelpBuilder setSource(String source) {
        this.source = source;
        return this;
    }

    public HelpBuilder setDelimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    public HelpBuilder addArg(String name, String desc, boolean isFacultative) {
        this.args.add(new Argument(name, desc, isFacultative));
        return this;
    }

    public HelpBuilder addArg(String name, boolean isFacultative) {
        return this.addArg(name, null, isFacultative);
    }

    public <T> HelpBuilder addArg(T[] options, boolean isFacultative) {
        return this.addArg(FormatUtils.format(options, Object::toString, "|"), null, isFacultative);
    }

    public HelpBuilder addField(String name, String value, boolean inline) {
        this.fields.add(new EmbedFieldEntity(name, value, inline));
        return this;
    }

    public Consumer<EmbedCreateSpec> build() {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> {
                    embed.setAuthor(String.format("Help for %s command", this.cmd.getName()),
                            null, this.context.getAvatarUrl());
                    embed.addField("Usage", this.getUsage(), false);

                    if (this.description != null && !this.description.isBlank()) {
                        embed.setDescription(this.description);
                    }

                    if (this.thumbnail != null && !this.thumbnail.isBlank()) {
                        embed.setThumbnail(this.thumbnail);
                    }

                    if (!this.getArguments().isEmpty()) {
                        embed.addField("Arguments", this.getArguments(), false);
                    }

                    if (this.example != null && !this.example.isBlank()) {
                        embed.addField("Example", this.example, false);
                    }

                    if (this.source != null && !this.source.isBlank()) {
                        embed.addField("Source", this.source, false);
                    }

                    for (final EmbedFieldEntity field : this.fields) {
                        embed.addField(field.getName(), field.getValue(), field.isInline());
                    }

                    if (this.cmd.getAlias() != null) {
                        embed.setFooter(String.format("Alias: %s", this.cmd.getAlias()), null);
                    }
                });
    }

    private String getUsage() {
        if (this.usage != null && !this.usage.isBlank()) {
            return String.format("`%s`", this.usage);
        }

        return String.format("`%s%s %s`",
                this.context.getPrefix(), this.cmd.getName(),
                FormatUtils.format(this.args, arg -> String.format(arg.isFacultative() ? "[<%s>]" : "<%s>", arg.getName()), this.delimiter));
    }

    private String getArguments() {
        return this.args.stream()
                .filter(arg -> Objects.nonNull(arg.getDescription()))
                .map(arg -> String.format("%n**%s** %s - %s", arg.getName(), arg.isFacultative() ? "[optional] " : "", arg.getDescription()))
                .collect(Collectors.joining());
    }
}