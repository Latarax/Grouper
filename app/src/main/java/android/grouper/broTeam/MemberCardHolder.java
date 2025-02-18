package android.grouper.broTeam;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MemberCardHolder extends RecyclerView.ViewHolder {

    TextView mTitle;
    ImageButton deleteMember;

    public MemberCardHolder(@NonNull View itemView) {
        super(itemView);

        this.mTitle = itemView.findViewById(R.id.titleTv);
        this.deleteMember = itemView.findViewById(R.id.deleteMemberBtn);
    }
}
