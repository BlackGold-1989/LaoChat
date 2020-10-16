package com.laodev.chatapp.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.laodev.chatapp.R;
import com.laodev.chatapp.activities.MainActivity;
import com.laodev.chatapp.adapters.LogCallAdapter;
import com.laodev.chatapp.interfaces.HomeIneractor;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.LogCall;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.views.MyRecyclerView;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class MyCallsFragment extends Fragment {

    private MyRecyclerView recyclerView, missedRecyclerView;
    private LogCallAdapter chatAdapter;
    private LogCallAdapter missedCallAdapter;

    private Realm rChatDb;
    private User userMe;
    private RealmResults<LogCall> resultList;
    private ArrayList<LogCall> callDataList = new ArrayList<>();
    private ArrayList<LogCall> logCallDataList = new ArrayList<>();
    private ArrayList<LogCall> missedCallDataList = new ArrayList<>();
    private LinearLayout emptyView;
    private TextView missedText;
    private TextView otherCallText;
    private NestedScrollView nestedScrollView;
    private FragmentManager manager;
    private Helper helper;


    private RealmChangeListener<RealmResults<LogCall>> chatListChangeListener = new RealmChangeListener<RealmResults<LogCall>>() {
        @Override
        public void onChange(@NonNull RealmResults<LogCall> element) {
            if (element.isValid() && element.size() > 0) {
                callDataList.clear();
                callDataList.addAll(rChatDb.copyFromRealm(element));

                for (int i = 0; i < callDataList.size(); i++) {
                    if (callDataList.get(i).getStatus().equalsIgnoreCase("CANCELED") ||
                            callDataList.get(i).getStatus().equalsIgnoreCase("DENIED")) {
                        missedCallDataList.add(callDataList.get(i));
                    } else {
                        logCallDataList.add(callDataList.get(i));
                    }
                }
                setUserNamesAsInPhone();
            }
        }
    };
    private HomeIneractor homeInteractor;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            homeInteractor = (HomeIneractor) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement HomeIneractor");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new Helper(getContext());
        userMe = homeInteractor.getUserMe();
        Realm.init(getContext());
        rChatDb = Helper.getRealmInstance();
        manager = getChildFragmentManager();
        Fragment frag = manager.findFragmentByTag("DELETE_TAG");
        if (frag != null) {
            manager.beginTransaction().remove(frag).commit();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        missedRecyclerView = view.findViewById(R.id.missedRecyclerView);
        missedText = view.findViewById(R.id.missedText);
        otherCallText = view.findViewById(R.id.otherCallText);
        emptyView = view.findViewById(R.id.emptyView);
        ImageView emptyImage = view.findViewById(R.id.emptyImage);
        nestedScrollView = view.findViewById(R.id.scroll);
        TextView emptyText = view.findViewById(R.id.emptyText);
        emptyImage.setBackgroundResource(R.drawable.ic_call_green_24dp);
        emptyText.setText(getString(R.string.empty_log_call_list));

        recyclerView.setNestedScrollingEnabled(false);
        missedRecyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        missedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            RealmQuery<LogCall> query = rChatDb.where(LogCall.class).equalTo("myId", userMe.getId());
            resultList = query.isNotNull("user").sort("timeUpdated", Sort.DESCENDING).findAll();

            logCallDataList.clear();
            missedCallDataList.clear();
            callDataList.clear();
            callDataList.addAll(rChatDb.copyFromRealm(resultList));

            for (int i = 0; i < callDataList.size(); i++) {
                if (callDataList.get(i).getStatus().equalsIgnoreCase("CANCELED") ||
                        callDataList.get(i).getStatus().equalsIgnoreCase("DENIED")) {
                    missedCallDataList.add(callDataList.get(i));
                } else {
                    logCallDataList.add(callDataList.get(i));
                }
            }

            chatAdapter = new LogCallAdapter(getActivity(), logCallDataList, MainActivity.myUsers,
                    helper.getLoggedInUser(), manager, helper);
            recyclerView.setAdapter(chatAdapter);
            missedCallAdapter = new LogCallAdapter(getActivity(), missedCallDataList,
                    MainActivity.myUsers, helper.getLoggedInUser(), manager, helper);
            missedRecyclerView.setAdapter(missedCallAdapter);

            resultList.addChangeListener(chatListChangeListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setUserNamesAsInPhone();
    }

    public void setUserNamesAsInPhone() {
        try {
            ArrayList<LogCall> tempList = new ArrayList<>();
            tempList.addAll(logCallDataList);
            tempList.addAll(missedCallDataList);
            if (homeInteractor != null) {
                for (LogCall logCall : tempList) {
                    User user = logCall.getUser();
                    if (user != null) {
                        if (helper.getCacheMyUsers() != null && helper.getCacheMyUsers().containsKey(user.getId())) {
                            user.setNameInPhone(helper.getCacheMyUsers().get(user.getId()).getNameToDisplay());
                        } else {
                            for (Contact savedContact : homeInteractor.getLocalContacts()) {
                                if (Helper.contactMatches(user.getId(), savedContact.getPhoneNumber())) {
                                    if (user.getNameInPhone() == null || !user.getNameInPhone().equals(savedContact.getName())) {
                                        user.setNameInPhone(savedContact.getName());
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (missedCallDataList.size() > 0) {
                missedText.setVisibility(View.VISIBLE);
            } else
                missedText.setVisibility(View.GONE);

            if (logCallDataList.size() > 0) {
                otherCallText.setVisibility(View.VISIBLE);
            } else
                otherCallText.setVisibility(View.GONE);

            if (missedCallDataList.size() == 0 && logCallDataList.size() == 0) {
                emptyView.setVisibility(View.VISIBLE);
                nestedScrollView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                nestedScrollView.setVisibility(View.VISIBLE);
            }

            if (chatAdapter != null)
                chatAdapter.notifyDataSetChanged();

            if (missedCallAdapter != null)
                missedCallAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            try {
                RealmQuery<LogCall> query = rChatDb.where(LogCall.class).equalTo("myId", userMe.getId());
                resultList = query.isNotNull("user").sort("timeUpdated", Sort.DESCENDING).findAll();

                logCallDataList.clear();
                missedCallDataList.clear();
                callDataList.clear();
                callDataList.addAll(rChatDb.copyFromRealm(resultList));

                for (int i = 0; i < callDataList.size(); i++) {
                    if (callDataList.get(i).getStatus().equalsIgnoreCase("CANCELED") ||
                            callDataList.get(i).getStatus().equalsIgnoreCase("DENIED")) {
                        missedCallDataList.add(callDataList.get(i));
                    } else {
                        logCallDataList.add(callDataList.get(i));
                    }
                }

                chatAdapter = new LogCallAdapter(getActivity(), logCallDataList, MainActivity.myUsers,
                        helper.getLoggedInUser(), manager, helper);
                recyclerView.setAdapter(chatAdapter);
                missedCallAdapter = new LogCallAdapter(getActivity(), missedCallDataList,
                        MainActivity.myUsers, helper.getLoggedInUser(), manager, helper);
                missedRecyclerView.setAdapter(missedCallAdapter);

                resultList.addChangeListener(chatListChangeListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setUserNamesAsInPhone();
        }
    }
}
