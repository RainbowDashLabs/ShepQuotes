package de.chojo.shepquotes.data.dao;

import de.chojo.sadu.base.QueryFactory;
import de.chojo.sadu.wrapper.QueryBuilderConfig;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class Post extends QueryFactory {
    private static final Logger log = getLogger(Post.class);
    private final QuoteChannel quoteChannel;
    private final Quote quote;
    private long messageId = -1L;

    public Post(QuoteChannel quoteChannel, Quote quote, long messageId) {
        super(quoteChannel.source(), QueryBuilderConfig.builder()
                .withExceptionHandler(err -> log.error("Unhandled exception", err))
                .build());
        this.quoteChannel = quoteChannel;
        this.quote = quote;
        this.messageId = messageId;
    }

    public Post(QuoteChannel quoteChannel, Quote quote) {
        super(quoteChannel.source(), QueryBuilderConfig.builder()
                .withExceptionHandler(err -> log.error("Unhandled exception", err))
                .build());
        this.quoteChannel = quoteChannel;
        this.quote = quote;
    }

    public void post() {
        if (messageId != -1L) {
            throw new IllegalStateException("Post is already posted");
        }

        quoteChannel.channel().ifPresent(channel -> {
            var message = channel.sendMessageEmbeds(quote.snapshot().embed()).complete();
            messageId = message.getIdLong();
            builder().query("""
                            INSERT INTO quote_posts(quote_id, message_id) VALUES (?,?)
                            """)
                    .parameter(stmt -> stmt.setInt(quote.id()).setLong(messageId))
                    .insert()
                    .send();
        });
    }

    public void delete() {
        quoteChannel.channel().ifPresent(channel -> channel.deleteMessageById(messageId)
                .queue(RestAction.getDefaultSuccess(),
                        ErrorResponseException.ignore(ErrorResponse.UNKNOWN_CHANNEL, ErrorResponse.UNKNOWN_MESSAGE)));
        builder().query("""
                        DELETE FROM quote_posts WHERE message_id = ?
                        """)
                .parameter(stmt -> stmt.setLong(messageId))
                .delete()
                .send();
    }

    public void update() {
        quoteChannel.channel()
                .ifPresent(channel -> channel.editMessageById(messageId, quote.snapshot().message()).complete());
    }
}
