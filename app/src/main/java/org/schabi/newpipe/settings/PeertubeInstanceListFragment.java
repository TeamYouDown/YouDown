package org.schabi.newpipe.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.grack.nanojson.JsonStringWriter;
import com.grack.nanojson.JsonWriter;

import org.schabi.newpipe.R;
import org.schabi.newpipe.databinding.DialogEditTextBinding;
import org.schabi.newpipe.databinding.FragmentInstanceListBinding;
import org.schabi.newpipe.databinding.ItemInstanceBinding;
import org.schabi.newpipe.extractor.services.peertube.PeertubeInstance;
import org.schabi.newpipe.util.Constants;
import org.schabi.newpipe.util.PeertubeHelper;
import org.schabi.newpipe.util.ThemeHelper;

import java.util.ArrayList;
import java.util.Collections;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PeertubeInstanceListFragment extends Fragment {
    private PeertubeInstance selectedInstance;
    private String savedInstanceListKey;
    private InstanceListAdapter instanceListAdapter;

    private FragmentInstanceListBinding binding;
    private SharedPreferences sharedPreferences;

    private CompositeDisposable disposables = new CompositeDisposable();

    /*//////////////////////////////////////////////////////////////////////////
    // Lifecycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        savedInstanceListKey = getString(R.string.peertube_instance_list_key);
        selectedInstance = PeertubeHelper.getCurrentInstance();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        binding = FragmentInstanceListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View rootView,
                              @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);

        binding.instanceHelpTV.setText(getString(R.string.peertube_instance_url_help,
                getString(R.string.peertube_instance_list_url)));
        binding.addInstanceButton.setOnClickListener(v -> showAddItemDialog(requireContext()));
        binding.instances.setLayoutManager(new LinearLayoutManager(requireContext()));

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(getItemTouchCallback());
        itemTouchHelper.attachToRecyclerView(binding.instances);

        instanceListAdapter = new InstanceListAdapter(requireContext(), itemTouchHelper);
        binding.instances.setAdapter(instanceListAdapter);
        instanceListAdapter.submitList(PeertubeHelper.getInstanceList(requireContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeHelper.setTitleToAppCompatActivity(getActivity(),
                getString(R.string.peertube_instance_url_title));
    }

    @Override
    public void onPause() {
        super.onPause();
        saveChanges();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposables != null) {
            disposables.clear();
        }
        disposables = null;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu,
                                    @NonNull final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_chooser_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_item_restore_default) {
            restoreDefaults();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    private void selectInstance(final PeertubeInstance instance) {
        selectedInstance = PeertubeHelper.selectInstance(instance, requireContext());
        sharedPreferences.edit().putBoolean(Constants.KEY_MAIN_PAGE_CHANGE, true).apply();
    }

    private void saveChanges() {
        final JsonStringWriter jsonWriter = JsonWriter.string().object().array("instances");
        for (final PeertubeInstance instance : instanceListAdapter.getCurrentList()) {
            jsonWriter.object();
            jsonWriter.value("name", instance.getName());
            jsonWriter.value("url", instance.getUrl());
            jsonWriter.end();
        }
        final String jsonToSave = jsonWriter.end().end().done();
        sharedPreferences.edit().putString(savedInstanceListKey, jsonToSave).apply();
    }

    private void restoreDefaults() {
        final Context context = requireContext();
        new AlertDialog.Builder(context)
                .setTitle(R.string.restore_defaults)
                .setMessage(R.string.restore_defaults_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    sharedPreferences.edit().remove(savedInstanceListKey).apply();
                    selectInstance(PeertubeInstance.DEFAULT_INSTANCE);
                    instanceListAdapter.submitList(PeertubeHelper.getInstanceList(context));
                })
                .show();
    }

    private void showAddItemDialog(final Context c) {
        final var dialogBinding = DialogEditTextBinding.inflate(getLayoutInflater());
        dialogBinding.dialogEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        dialogBinding.dialogEditText.setHint(R.string.peertube_instance_add_help);

        new AlertDialog.Builder(c)
                .setTitle(R.string.peertube_instance_add_title)
                .setIcon(R.drawable.ic_placeholder_peertube)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog1, which) -> {
                    final String url = dialogBinding.dialogEditText.getText().toString();
                    addInstance(url);
                })
                .show();
    }

    private void addInstance(final String url) {
        final String cleanUrl = cleanUrl(url);
        if (cleanUrl == null) {
            return;
        }
        binding.loadingProgressBar.setVisibility(View.VISIBLE);
        final Disposable disposable = Single.fromCallable(() -> {
            final PeertubeInstance instance = new PeertubeInstance(cleanUrl);
            instance.fetchInstanceMetaData();
            return instance;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe((instance) -> {
                    binding.loadingProgressBar.setVisibility(View.GONE);
                    add(instance);
                }, e -> {
                    binding.loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), R.string.peertube_instance_add_fail,
                            Toast.LENGTH_SHORT).show();
                });
        disposables.add(disposable);
    }

    @Nullable
    private String cleanUrl(final String url) {
        String cleanUrl = url.trim();
        // if protocol not present, add https
        if (!cleanUrl.startsWith("http")) {
            cleanUrl = "https://" + cleanUrl;
        }
        // remove trailing slash
        cleanUrl = cleanUrl.replaceAll("/$", "");
        // only allow https
        if (!cleanUrl.startsWith("https://")) {
            Toast.makeText(getActivity(), R.string.peertube_instance_add_https_only,
                    Toast.LENGTH_SHORT).show();
            return null;
        }
        // only allow if not already exists
        for (final PeertubeInstance instance : instanceListAdapter.getCurrentList()) {
            if (instance.getUrl().equals(cleanUrl)) {
                Toast.makeText(getActivity(), R.string.peertube_instance_add_exists,
                        Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return cleanUrl;
    }

    private void add(final PeertubeInstance instance) {
        final var list = new ArrayList<>(instanceListAdapter.getCurrentList());
        list.add(instance);
        instanceListAdapter.submitList(list);
    }

    private ItemTouchHelper.SimpleCallback getItemTouchCallback() {
        return new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public int interpolateOutOfBoundsScroll(@NonNull final RecyclerView recyclerView,
                                                    final int viewSize,
                                                    final int viewSizeOutOfBounds,
                                                    final int totalSize,
                                                    final long msSinceStartScroll) {
                final int standardSpeed = super.interpolateOutOfBoundsScroll(recyclerView, viewSize,
                        viewSizeOutOfBounds, totalSize, msSinceStartScroll);
                final int minimumAbsVelocity = Math.max(12, Math.abs(standardSpeed));
                return minimumAbsVelocity * (int) Math.signum(viewSizeOutOfBounds);
            }

            @Override
            public boolean onMove(@NonNull final RecyclerView recyclerView,
                                  @NonNull final RecyclerView.ViewHolder source,
                                  @NonNull final RecyclerView.ViewHolder target) {
                if (source.getItemViewType() != target.getItemViewType()
                        || instanceListAdapter == null) {
                    return false;
                }

                final int sourceIndex = source.getBindingAdapterPosition();
                final int targetIndex = target.getBindingAdapterPosition();
                instanceListAdapter.swapItems(sourceIndex, targetIndex);
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
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder,
                                 final int swipeDir) {
                final int position = viewHolder.getBindingAdapterPosition();
                // do not allow swiping the selected instance
                if (instanceListAdapter.getCurrentList().get(position).getUrl()
                        .equals(selectedInstance.getUrl())) {
                    instanceListAdapter.notifyItemChanged(position);
                    return;
                }
                final var list = new ArrayList<>(instanceListAdapter.getCurrentList());
                list.remove(position);

                if (list.isEmpty()) {
                    list.add(selectedInstance);
                }

                instanceListAdapter.submitList(list);
            }
        };
    }

    /*//////////////////////////////////////////////////////////////////////////
    // List Handling
    //////////////////////////////////////////////////////////////////////////*/

    private class InstanceListAdapter
            extends ListAdapter<PeertubeInstance, InstanceListAdapter.TabViewHolder> {
        private final LayoutInflater inflater;
        private final ItemTouchHelper itemTouchHelper;
        private RadioButton lastChecked;

        InstanceListAdapter(final Context context, final ItemTouchHelper itemTouchHelper) {
            super(new PeertubeInstanceCallback());
            this.itemTouchHelper = itemTouchHelper;
            this.inflater = LayoutInflater.from(context);
        }

        public void swapItems(final int fromPosition, final int toPosition) {
            final var list = new ArrayList<>(getCurrentList());
            Collections.swap(list, fromPosition, toPosition);
            submitList(list);
        }

        @NonNull
        @Override
        public InstanceListAdapter.TabViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                                                    final int viewType) {
            return new InstanceListAdapter.TabViewHolder(ItemInstanceBinding.inflate(inflater,
                    parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final InstanceListAdapter.TabViewHolder holder,
                                     final int position) {
            holder.bind(position);
        }

        class TabViewHolder extends RecyclerView.ViewHolder {
            private final ItemInstanceBinding itemBinding;

            TabViewHolder(final ItemInstanceBinding binding) {
                super(binding.getRoot());
                this.itemBinding = binding;
            }

            @SuppressLint("ClickableViewAccessibility")
            void bind(final int position) {
                itemBinding.handle.setOnTouchListener((view, motionEvent) -> {
                    if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        if (itemTouchHelper != null && getItemCount() > 1) {
                            itemTouchHelper.startDrag(this);
                            return true;
                        }
                    }
                    return false;
                });

                final PeertubeInstance instance = getItem(position);
                itemBinding.instanceName.setText(instance.getName());
                itemBinding.instanceUrl.setText(instance.getUrl());
                itemBinding.selectInstanceRB.setOnCheckedChangeListener(null);
                if (selectedInstance.getUrl().equals(instance.getUrl())) {
                    if (lastChecked != null && lastChecked != itemBinding.selectInstanceRB) {
                        lastChecked.setChecked(false);
                    }
                    itemBinding.selectInstanceRB.setChecked(true);
                    lastChecked = itemBinding.selectInstanceRB;
                }
                itemBinding.selectInstanceRB.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectInstance(instance);
                        if (lastChecked != null && lastChecked != itemBinding.selectInstanceRB) {
                            lastChecked.setChecked(false);
                        }
                        lastChecked = itemBinding.selectInstanceRB;
                    }
                });
                itemBinding.instanceIcon.setImageResource(R.drawable.ic_placeholder_peertube);
            }
        }
    }

    private static class PeertubeInstanceCallback extends DiffUtil.ItemCallback<PeertubeInstance> {
        @Override
        public boolean areItemsTheSame(@NonNull final PeertubeInstance oldItem,
                                       @NonNull final PeertubeInstance newItem) {
            return oldItem.getUrl().equals(newItem.getUrl());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final PeertubeInstance oldItem,
                                          @NonNull final PeertubeInstance newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getUrl().equals(newItem.getUrl());
        }
    }
}
