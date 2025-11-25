package com.example.form;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//画面用のForm
public class OthersReviewForm {
    @NotBlank
    public String url;
    public String previewTitle;
    public String previewImage;
    // 投稿用
    @Min(1)
    @Max(5)
    public Integer rate;
    @NotBlank
    public String reviewTitle;
    @NotBlank
    public String reviewText;

    // 必須化
    @NotNull(message = "カテゴリを選択してください")
    private List<Integer> categoryIds;
}
