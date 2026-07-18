package com.indorcallejero.api.facebookvideo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Sin foto: a diferencia de Sponsor/Information, esto es un link a un
// video ya alojado en Facebook, no un archivo que suba esta API.
@Entity
@Table(name = "facebook_videos")
public class FacebookVideoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String videoUrl;

    protected FacebookVideoEntity() {
    }

    public FacebookVideoEntity(String title, String videoUrl) {
        this.title = title;
        this.videoUrl = videoUrl;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
