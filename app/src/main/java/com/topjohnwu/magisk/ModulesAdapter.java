package com.topjohnwu.magisk;

import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.topjohnwu.magisk.module.Module;
import com.topjohnwu.magisk.utils.Shell;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ModulesAdapter extends RecyclerView.Adapter<ModulesAdapter.ViewHolder> {

    private final List<Module> mList;
    private View mView;
    private Context context;

    public ModulesAdapter(List<Module> list) {
        mList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_module, parent, false);
        context = parent.getContext();
        ButterKnife.bind(this, mView);
        return new ViewHolder(mView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Module module = mList.get(position);
        if (module.isCache()) {
            holder.title.setText("[Cache] " + module.getName());
        } else {
            holder.title.setText(module.getName());
        }
        String author = module.getAuthor();
        String versionName = module.getVersion();
        String description = module.getDescription();
        if (versionName != null) {
            holder.versionName.setText(versionName);
        }
        if (author != null) {
            holder.author.setText(context.getString(R.string.author, author));
        }
        if (description != null) {
            holder.description.setText(description);
        }

        holder.checkBox.setChecked(module.isEnabled());
        holder.checkBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        module.removeDisableFile();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void v) {
                        Snackbar.make(mView, R.string.disable_file_removed, Snackbar.LENGTH_SHORT).show();
                    }
                }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            } else {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        module.createDisableFile();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void v) {
                        Snackbar.make(mView, R.string.disable_file_created, Snackbar.LENGTH_SHORT).show();
                    }
                }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        });

        holder.delete.setOnClickListener(v -> {
            if (module.willBeRemoved()) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        module.deleteRemoveFile();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void v) {
                        Snackbar.make(mView, R.string.remove_file_deleted, Snackbar.LENGTH_SHORT).show();
                        updateDeleteButton(holder, module);
                    }
                }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            } else {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        module.createRemoveFile();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void v) {
                        Snackbar.make(mView, R.string.remove_file_created, Snackbar.LENGTH_SHORT).show();
                        updateDeleteButton(holder, module);
                    }
                }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        });

        if (module.isUpdated()) {
            holder.notice.setVisibility(View.VISIBLE);
            holder.notice.setText(R.string.update_file_created);
            holder.delete.setEnabled(false);
        } else {
            updateDeleteButton(holder, module);
        }
    }

    private void updateDeleteButton(ViewHolder holder, Module module) {
        holder.notice.setVisibility(module.willBeRemoved() ? View.VISIBLE : View.GONE);

        if (module.willBeRemoved()) {
            holder.delete.setImageResource(R.drawable.ic_undelete);
        } else {
            holder.delete.setImageResource(R.drawable.ic_delete);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.title) TextView title;
        @BindView(R.id.version_name) TextView versionName;
        @BindView(R.id.description) TextView description;
        @BindView(R.id.notice) TextView notice;
        @BindView(R.id.checkbox) CheckBox checkBox;
        @BindView(R.id.author) TextView author;
        @BindView(R.id.delete) ImageView delete;

        public ViewHolder(View itemView) {
            super(itemView);
            WindowManager windowmanager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            ButterKnife.bind(this, itemView);
            DisplayMetrics dimension = new DisplayMetrics();
            windowmanager.getDefaultDisplay().getMetrics(dimension);

            if (!Shell.rootAccess()) {
                checkBox.setEnabled(false);
                delete.setEnabled(false);
            }
        }
    }
}
