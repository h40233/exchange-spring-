package com.exchange.exchange.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ====== 檔案總結 ======
// Member 實體類別對應 `members` 表。
// 紀錄使用者的基本帳戶資訊。
// 安全性設計：使用 @JsonProperty(WRITE_ONLY) 防止密碼欄位在 API 回應中被序列化輸出。
@Entity
@Table(name = "members")
public class Member {

    // 會員 ID (Primary Key)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memberID")
    private Integer memberId;

    // 登入帳號 (通常為 Email)，設為唯一索引
    @Column(length = 45, unique = true)
    private String account;

    // 登入密碼
    // WRITE_ONLY: 允許從 JSON 讀入 (註冊/登入時)，但生成 JSON 時會忽略此欄位 (不回傳給前端)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(length = 45)
    private String password;

    // 使用者姓名
    @Column(length = 45)
    private String name;

    // 聯絡電話
    @Column(length = 45)
    private String number;

    // 註冊時間
    @Column(name = "join_time")
    private LocalDateTime joinTime;

    public Member() {
    }

    // --- Getters & Setters ---

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }
}