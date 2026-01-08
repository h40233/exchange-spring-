package com.exchange.exchange.service;

// 引入實體：會員
import com.exchange.exchange.entity.Member;
// 引入資料存取層
import com.exchange.exchange.repository.MemberRepository;
// 引入 Spring 工具
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ====== 檔案總結 ======
// MemberService 提供會員帳號管理的核心功能。
// 包含帳號註冊 (去空白、防重)、登入驗證 (密碼比對) 與個人資料更新。
@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    // 方法：註冊新會員
    public Member register(String account, String password, String name, String number) {
        // 1. 資料清洗：去除前後空白，避免 "admin " 與 "admin" 被視為不同帳號
        String cleanAccount = (account != null) ? account.trim() : "";
        String cleanPassword = (password != null) ? password.trim() : "";

        // 2. 防重檢查：若帳號已存在則拋出異常
        if (memberRepository.findByAccount(cleanAccount).isPresent()) {
            throw new RuntimeException("Account already exists");
        }
        
        // 3. 建立實體並存檔
        Member member = new Member();
        member.setAccount(cleanAccount);
        member.setPassword(cleanPassword); // [註1]
        member.setName(name);
        member.setNumber(number);
        member.setJoinTime(LocalDateTime.now()); // 設定註冊時間
        return memberRepository.save(member);
    }

    // 方法：會員登入
    public Member login(String account, String password) {
        // 資料清洗
        String cleanAccount = account.trim();
        String cleanPassword = password.trim();

        // 用於除錯的日誌輸出 (生產環境建議移除或遮蔽敏感資訊)
        System.out.println("嘗試登入 - 帳號: [" + cleanAccount + "]");

        // 查詢會員，若不存在則回傳 null
        Member member = memberRepository.findByAccount(cleanAccount).orElse(null);
        if (member == null) {
            System.out.println("登入失敗: 找不到帳號");
            throw new IllegalArgumentException("Invalid account or password");
        }
        
        // 密碼比對：直接比較明文密碼 [註1]
        if (member.getPassword().equals(cleanPassword)) {
            System.out.println("登入成功!");
            // 實際專案中通常在此處生成 JWT Token 或 Session
            return member;
        } else {
            // 登入失敗處理與除錯資訊
            System.out.println("登入失敗: 密碼不符");
            System.out.println("輸入密碼長度: " + cleanPassword.length());
            System.out.println("儲存密碼長度: " + member.getPassword().length());
            throw new IllegalArgumentException("Invalid account or password");
        }
    }

    // 方法：更新會員資料
    public Member updateMember(Integer id, String name, String number, String password) {
        // 使用 map 來處理 Optional，若存在則更新，否則回傳 null
        return memberRepository.findById(id).map(member -> {
            // 僅更新非空值欄位 (Partial Update)
            if (name != null) member.setName(name);
            if (number != null) member.setNumber(number);
            
            // 更新密碼：確保非空且不僅僅是空白字元
            if (password != null && !password.trim().isEmpty()) {
                member.setPassword(password.trim());
            }
            return memberRepository.save(member);
        }).orElse(null);
    }

    // 方法：獲取所有會員列表 (僅供管理員使用，需留意資料量)
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    // 方法：根據 ID 獲取單一會員
    public Optional<Member> getMemberById(Integer id) {
        return memberRepository.findById(id);
    }
}

// ====== 備註區 ======
/*
[註1] 重大安全隱患 (Security Risk):
      目前的實作直接儲存並比對「明文密碼」。這是極度危險的。
      改進方案：
      1. 引入 Spring Security Crypto。
      2. 註冊時：member.setPassword(passwordEncoder.encode(rawPassword));
      3. 登入時：passwordEncoder.matches(rawPassword, member.getPassword());
      4. 絕對不要在日誌 (System.out) 中印出密碼或其長度。

[註2] 交易事務 (Transactions):
      雖然單表操作通常具有原子性，但建議在 register 與 updateMember 加上 @Transactional，
      以確保未來擴充邏輯 (如註冊送體驗金) 時的資料一致性。
*/