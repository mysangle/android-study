package com.twentyhours.androidstudy.recyclerview;

/**
 * Created by soonhyung-imac on 4/5/17.
 */

public class AdapterHelper implements OpReorderer.Callback {
  final Callback mCallback;
  final boolean mDisableRecycler;
  final OpReorderer mOpReorderer;

  AdapterHelper(Callback callback) {
    this(callback, false);
  }

  AdapterHelper(Callback callback, boolean disableRecycler) {
    mCallback = callback;
    mDisableRecycler = disableRecycler;
    mOpReorderer = new OpReorderer(this);
  }

  @Override
  public UpdateOp obtainUpdateOp(int cmd, int startPosition, int itemCount, Object payload) {
    return null;
  }

  @Override
  public void recycleUpdateOp(UpdateOp op) {

  }

  static class UpdateOp {

  }

  interface Callback {
    RecyclerView.ViewHolder findViewHolder(int position);

    void offsetPositionsForRemovingInvisible(int positionStart, int itemCount);

    void offsetPositionsForRemovingLaidOutOrNewView(int positionStart, int itemCount);

    void markViewHoldersUpdated(int positionStart, int itemCount, Object payloads);

    void onDispatchFirstPass(UpdateOp updateOp);

    void onDispatchSecondPass(UpdateOp updateOp);

    void offsetPositionsForAdd(int positionStart, int itemCount);

    void offsetPositionsForMove(int from, int to);
  }
}
