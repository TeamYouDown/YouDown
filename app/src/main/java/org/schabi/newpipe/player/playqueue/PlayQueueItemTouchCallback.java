package org.schabi.newpipe.player.playqueue;

import androidx.annotation.NonNull;
import androidx.core.math.MathUtils;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public abstract class PlayQueueItemTouchCallback extends ItemTouchHelper.SimpleCallback {
    private static final int MINIMUM_INITIAL_DRAG_VELOCITY = 10;
    private static final int MAXIMUM_INITIAL_DRAG_VELOCITY = 25;

    public PlayQueueItemTouchCallback() {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT);
    }

    public abstract void onMove(int sourceIndex, int targetIndex);

    public abstract void onSwiped(int index);

    @Override
    public int interpolateOutOfBoundsScroll(@NonNull final RecyclerView recyclerView,
                                            final int viewSize,
                                            final int viewSizeOutOfBounds,
                                            final int totalSize,
                                            final long msSinceStartScroll) {
        final int standardSpeed = super.interpolateOutOfBoundsScroll(recyclerView, viewSize,
                viewSizeOutOfBounds, totalSize, msSinceStartScroll);
        final int clampedAbsVelocity = MathUtils.clamp(Math.abs(standardSpeed),
                MINIMUM_INITIAL_DRAG_VELOCITY, MAXIMUM_INITIAL_DRAG_VELOCITY);
        return clampedAbsVelocity * (int) Math.signum(viewSizeOutOfBounds);
    }

    @Override
    public boolean onMove(@NonNull final RecyclerView recyclerView,
                          final RecyclerView.ViewHolder source,
                          final RecyclerView.ViewHolder target) {
        if (source.getItemViewType() != target.getItemViewType()) {
            return false;
        }

        final int sourceIndex = source.getLayoutPosition();
        final int targetIndex = target.getLayoutPosition();
        onMove(sourceIndex, targetIndex);
        return true;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, final int swipeDir) {
        onSwiped(viewHolder.getBindingAdapterPosition());
    }
}
