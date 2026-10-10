package com.mandiwatch.web;

import com.mandiwatch.domain.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FarmerForm {

    @NotBlank(message = "Name is required")
    @Size(max = 60, message = "Name is too long")
    private String name;

    @Pattern(regexp = "[6-9]\\d{9}", message = "Enter a 10-digit Indian mobile number")
    private String phone;

    @NotNull(message = "Pick a village")
    private Long villageId;

    @NotNull
    private Language language = Language.TELUGU;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Long getVillageId() { return villageId; }
    public void setVillageId(Long villageId) { this.villageId = villageId; }
    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }
}
