package com.exchange.exchange.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

// ====== 檔案總結 ======
// Member 實體對應 `members` 表。
// 紀錄使用者的帳號資訊。使用了 WRITE_ONLY 屬性來保護密碼不被 API 輸出。
@Entity
@Table(name = "members")
public class Member {

    // 會員 ID (主鍵)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memberID")
    private Integer memberId;

    // 帳號 (唯一)
    @Column(length = 45, unique = true)
    private String account;

    // 密碼
    // 安全性標記：JSON 序列化時忽略此欄位 (只進不出)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(length = 45)
    private String password;

    // 姓名
    @Column(length = 45)
    private String name;

    // 電話號碼
    @Column(length = 45)
    private String number;

    // 註冊時間
    @Column(name = "join_time")
    private LocalDateTime joinTime;

    // 無參數建構子
    public Member() {
    }

    // 取得會員 ID
    public Integer getMemberId() {
        return memberId;
    }

    // 設定會員 ID
    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    // 取得帳號
    public String getAccount() {
        return account;
    }

    // 設定帳號
    public void setAccount(String account) {
        this.account = account;
    }

    // 取得密碼
    public String getPassword() {
        return password;
    }

    // 設定密碼
    public void setPassword(String password) {
        this.password = password;
    }

    // 取得姓名
    public String getName() {
        return name;
    }

    // 設定姓名
    public void setName(String name) {
        this.name = name;
    }

    // 取得電話
    public String getNumber() {
        return number;
    }

    // 設定電話
    public void setNumber(String number) {
        this.number = number;
    }

    // 取得註冊時間
    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    // 設定註冊時間
    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }
}