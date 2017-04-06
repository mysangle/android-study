package com.twentyhours.androidstudy.recyclerview;

/**
 * Created by soonhyung-imac on 4/5/17.
 */

public class OpReorderer {
  final Callback mCallback;

  public OpReorderer(Callback callback) {
    mCallback = callback;
  }

  interface Callback {
    AdapterHelper.UpdateOp obtainUpdateOp(int cmd, int startPosition, int itemCount, Object payload);

    void recycleUpdateOp(AdapterHelper.UpdateOp op);
  }
}
