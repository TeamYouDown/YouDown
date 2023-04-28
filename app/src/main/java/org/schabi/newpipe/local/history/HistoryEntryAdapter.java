package org.schabi.newpipe.local.history;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.schabi.newpipe.util.Localization;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;


/**
 * This is an adapter for history entries.
 *
 * @param <E>  the type of the entries
 * @param <VH> the type of the view holder
 */
public abstract class HistoryEntryAdapter<E, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private final ArrayList<E> mEntries;
    private final DateFormat mDateFormat;
    private final Context mContext;
    private OnHistoryItemClickListener<E> onHistoryItemClickListener = null;

    public HistoryEntryAdapter(final Context context) {
        super();
        mContext = context;
        mEntries = new ArrayList<>();
        mDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM,
                Localization.getPreferredLocale(context));
    }

    public void setEntries(@NonNull final Collection<E> historyEntries) {
        mEntries.clear();
        mEntries.addAll(historyEntries);
        notifyDataSetChanged();
    }

    public Collection<E> getItems() {
        return mEntries;
    }

    public void clear() {
        mEntries.clear();
        notifyDataSetChanged();
    }

    protected String getFormattedDate(final Date date) {
        return mDateFormat.format(date);
    }

    protected String getFormattedViewString(final long viewCount) {
        return Localization.shortViewCount(mContext, viewCount);
    }

    @Override
    public int getItemCount() {
        return mEntries.size();
    }

    @Override
    public void onBindViewHolder(final VH holder, final int position) {
        final E entry = mEntries.get(position);
        holder.itemView.setOnClickListener(v -> {
            if (onHistoryItemClickListener != null) {
                onHistoryItemClickListener.onHistoryItemClick(entry);
            }
        });

        holder.itemView.setOnLongClickListener(view -> {
            if (onHistoryItemClickListener != null) {
                onHistoryItemClickListener.onHistoryItemLongClick(entry);
                return true;
            }
            return false;
        });

        onBindViewHolder(holder, entry, position);
    }

    @Override
    public void onViewRecycled(@NonNull final VH holder) {
        super.onViewRecycled(holder);
        holder.itemView.setOnClickListener(null);
    }

    abstract void onBindViewHolder(VH holder, E entry, int position);

    public void setOnHistoryItemClickListener(
            @Nullable final OnHistoryItemClickListener<E> onHistoryItemClickListener) {
        this.onHistoryItemClickListener = onHistoryItemClickListener;
    }

    public boolean isEmpty() {
        return mEntries.isEmpty();
    }

    public interface OnHistoryItemClickListener<E> {
        void onHistoryItemClick(E item);

        void onHistoryItemLongClick(E item);
    }
}
