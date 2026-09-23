package com.example.smartduetadmissionsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AdminManagementAdapter
        extends RecyclerView.Adapter<AdminManagementAdapter.ViewHolder> {

    public interface OnDisableClickListener {
        void onDisable(
                AdminManagementActivity.AdminModel admin
        );
    }

    private final Context context;
    private final List<AdminManagementActivity.AdminModel> list;
    private final OnDisableClickListener listener;

    public AdminManagementAdapter(
            Context context,
            List<AdminManagementActivity.AdminModel> list,
            OnDisableClickListener listener
    ) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(context)
                        .inflate(
                                R.layout.item_admin_management,
                                parent,
                                false
                        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        AdminManagementActivity.AdminModel admin =
                list.get(position);

        holder.tvName.setText(
                admin.name.isEmpty()
                        ? "Admin"
                        : admin.name
        );

        holder.tvEmail.setText(
                admin.email.isEmpty()
                        ? "Email not available"
                        : admin.email
        );

        holder.tvRole.setText(
                admin.role.equalsIgnoreCase("admin")
                        ? "Administrator"
                        : admin.role
        );

        holder.tvStatus.setText(
                admin.active
                        ? "Active"
                        : "Inactive"
        );

        holder.btnDisable.setEnabled(
                admin.active
        );

        holder.btnDisable.setText(
                admin.active
                        ? "Disable Access"
                        : "Disabled"
        );

        holder.btnDisable.setOnClickListener(
                v -> listener.onDisable(admin)
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvEmail;
        TextView tvRole;
        TextView tvStatus;

        MaterialButton btnDisable;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName =
                    itemView.findViewById(
                            R.id.tvAdminName
                    );

            tvEmail =
                    itemView.findViewById(
                            R.id.tvAdminEmail
                    );

            tvRole =
                    itemView.findViewById(
                            R.id.tvAdminRole
                    );

            tvStatus =
                    itemView.findViewById(
                            R.id.tvAdminStatus
                    );

            btnDisable =
                    itemView.findViewById(
                            R.id.btnDisableAdmin
                    );
        }
    }
}