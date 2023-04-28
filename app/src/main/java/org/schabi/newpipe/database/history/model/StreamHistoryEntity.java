package org.schabi.newpipe.database.history.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import org.schabi.newpipe.database.stream.model.StreamEntity;

import java.time.OffsetDateTime;

import static androidx.room.ForeignKey.CASCADE;
import static org.schabi.newpipe.database.history.model.StreamHistoryEntity.JOIN_STREAM_ID;
import static org.schabi.newpipe.database.history.model.StreamHistoryEntity.STREAM_ACCESS_DATE;
import static org.schabi.newpipe.database.history.model.StreamHistoryEntity.STREAM_HISTORY_TABLE;

@Entity(tableName = STREAM_HISTORY_TABLE,
        primaryKeys = {JOIN_STREAM_ID, STREAM_ACCESS_DATE},
        // No need to index for timestamp as they will almost always be unique
        indices = {@Index(value = {JOIN_STREAM_ID})},
        foreignKeys = {
                @ForeignKey(entity = StreamEntity.class,
                        parentColumns = StreamEntity.STREAM_ID,
                        childColumns = JOIN_STREAM_ID,
                        onDelete = CASCADE, onUpdate = CASCADE)
        })
public class StreamHistoryEntity {
    public static final String STREAM_HISTORY_TABLE = "stream_history";
    public static final String JOIN_STREAM_ID = "stream_id";
    public static final String STREAM_ACCESS_DATE = "access_date";
    public static final String STREAM_REPEAT_COUNT = "repeat_count";

    @ColumnInfo(name = JOIN_STREAM_ID)
    private long streamUid;

    @NonNull
    @ColumnInfo(name = STREAM_ACCESS_DATE)
    private OffsetDateTime accessDate;

    @ColumnInfo(name = STREAM_REPEAT_COUNT)
    private long repeatCount;

    /**
     * @param streamUid the stream id this history item will refer to
     * @param accessDate the last time the stream was accessed
     * @param repeatCount the total number of views this stream received
     */
    public StreamHistoryEntity(final long streamUid,
                               @NonNull final OffsetDateTime accessDate,
                               final long repeatCount) {
        this.streamUid = streamUid;
        this.accessDate = accessDate;
        this.repeatCount = repeatCount;
    }

    public long getStreamUid() {
        return streamUid;
    }

    public void setStreamUid(final long streamUid) {
        this.streamUid = streamUid;
    }

    @NonNull
    public OffsetDateTime getAccessDate() {
        return accessDate;
    }

    public void setAccessDate(@NonNull final OffsetDateTime accessDate) {
        this.accessDate = accessDate;
    }

    public long getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(final long repeatCount) {
        this.repeatCount = repeatCount;
    }
}
