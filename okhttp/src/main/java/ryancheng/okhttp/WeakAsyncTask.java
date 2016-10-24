package ryancheng.okhttp;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

/**
 * Created by ryan on 2015/1/11.
 */
public abstract class WeakAsyncTask<Params, Progress, Result, WeakTarget> extends
        AsyncTask<Params, Progress, Result> {
    protected WeakReference<WeakTarget> mTarget;

    public WeakAsyncTask(WeakTarget target) {
        mTarget = new WeakReference<>(target);
    }

    @Override
    protected final void onPreExecute() {
        final WeakTarget target = mTarget.get();
        if (target != null) {
            this.onPreExecute(target);
        }
    }

    @Override
    protected final Result doInBackground(Params... params) {
        final WeakTarget target = mTarget.get();
        if (target != null) {
            return this.doInBackground(target, params);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void onPostExecute(Result result) {
        final WeakTarget target = mTarget.get();
        if (target != null) {
            this.onPostExecute(target, result);
        }
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        final WeakTarget target = mTarget.get();
        if (target != null) {
            this.onProgressUpdate(target, values);
        }
    }

    @Override
    protected void onCancelled(Result result) {
        final WeakTarget target = mTarget.get();
        if (target != null) {
            this.onCancelled(target, result);
        }
    }

    protected void onPreExecute(WeakTarget target) {
        // No default action
    }

    protected abstract Result doInBackground(WeakTarget target, Params... params);

    protected void onPostExecute(WeakTarget target, Result result) {
        // No default action
    }

    protected void onProgressUpdate(WeakTarget target, Progress... values) {
        // No default action
    }

    protected void onCancelled(WeakTarget target, Result result) {
        // No default action
    }
}
