package android.grouper.broTeam;

import android.view.View;

public class CardModel {

    private View.OnClickListener onClickListener;
    private String title, description, identification;
    private int img;

    public String getTitle(){
        return title;
    }

    public String getDescription(){
        return description;
    }

    public int getImg(){
        return img;
    }

    public String getIdentification(){ return identification; }

    public void setTitle(String title){
        this.title = title;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public void setImg(int img) {
        this.img = img;
    }

    public void setIdentification(String gid){this.identification = gid; }
}

