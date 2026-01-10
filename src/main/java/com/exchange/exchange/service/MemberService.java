package com.exchange.exchange.service;

// 引入實體：會員
import com.exchange.exchange.entity.Member;
// 引入資料存取層：會員儲存庫
import com.exchange.exchange.repository.MemberRepository;
// 引入 Spring 依賴注入與服務註解
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 引入時間與集合工具
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ====== 檔案總結 ======
// MemberService 封裝了會員帳戶管理的業務邏輯。
// 核心職責：
// 1. 帳號註冊：包含輸入資料清洗、帳號重複檢查、實體建立。
// 2. 登入驗證：包含帳號查詢、密碼比對 (目前為明文比對)。
// 3. 資料維護：提供會員資料的查詢與部分更新功能。
@Service
public class MemberService {

    // 注入會員資料庫操作介面
    @Autowired
    private MemberRepository memberRepository;

    // 方法：註冊新會員
    // 輸入：帳號、密碼、姓名、電話
    // 輸出：建立成功的會員實體
    public Member register(String account, String password, String name, String number) {
        // 資料清洗：去除帳號與密碼前後的空白字元
        // 這是為了防止使用者誤輸入空格導致無法登入 (例如 "admin " 與 "admin")
        String cleanAccount = (account != null) ? account.trim() : "";
        String cleanPassword = (password != null) ? password.trim() : "";

        // 邏輯判斷：檢查帳號是否已存在
        // 若已存在則拋出 RuntimeException 中斷流程
        if (memberRepository.findByAccount(cleanAccount).isPresent()) {
            throw new RuntimeException("Account already exists");
        }
        
        // 建立會員實體並填入資料
        Member member = new Member();
        member.setAccount(cleanAccount);
        // 設定密碼：目前直接儲存明碼，有極大安全風險 [註1]
        member.setPassword(cleanPassword); 
        member.setName(name);
        member.setNumber(number);
        // 設定註冊時間為當前系統時間
        member.setJoinTime(LocalDateTime.now()); 
        
        // 寫入資料庫並回傳
        return memberRepository.save(member);
    }

    // 方法：會員登入
    // 輸入：帳號、密碼
    // 輸出：登入成功的會員實體 (若失敗則拋出異常)
    public Member login(String account, String password) {
        // 資料清洗：去除輸入的空白
        String cleanAccount = account.trim();
        String cleanPassword = password.trim();

        // 偵錯日誌：印出嘗試登入的帳號 (生產環境應避免印出此類資訊)
        System.out.println("嘗試登入 - 帳號: [" + cleanAccount + "]");

        // 步驟 1：根據帳號查詢會員
        // 若找不到該帳號，回傳 null
        Member member = memberRepository.findByAccount(cleanAccount).orElse(null);
        
        // 邏輯判斷：若會員不存在，印出失敗訊息並拋出異常
        if (member == null) {
            System.out.println("登入失敗: 找不到帳號");
            throw new IllegalArgumentException("Invalid account or password");
        }
        
        // 步驟 2：比對密碼
        // 直接使用字串 equals 比較輸入密碼與資料庫密碼 [註1]
        if (member.getPassword().equals(cleanPassword)) {
            System.out.println("登入成功!");
            // 登入成功，回傳會員實體
            return member;
        } else {
            // 登入失敗處理
            // 印出詳細的長度資訊以供除錯 (例如檢查是否有看不見的特殊字元)
            System.out.println("登入失敗: 密碼不符");
            System.out.println("輸入密碼長度: " + cleanPassword.length());
            System.out.println("儲存密碼長度: " + member.getPassword().length());
            // 拋出異常，訊息模糊化以防止帳號列舉攻擊
            throw new IllegalArgumentException("Invalid account or password");
        }
    }

    // 方法：更新會員資料 (部分更新)
    // 輸入：ID, 姓名, 電話, 密碼 (可為 null，表示不更新該欄位)
    public Member updateMember(Integer id, String name, String number, String password) {
        // 使用 map 操作 Optional：若 ID 存在則執行更新邏輯，否則回傳 null
        return memberRepository.findById(id).map(member -> {
            // 邏輯判斷：僅當傳入參數不為 null 時才更新對應欄位 (Partial Update Pattern)
            if (name != null) member.setName(name);
            if (number != null) member.setNumber(number);
            
            // 邏輯判斷：更新密碼前需確保非 null 且非空字串
            if (password != null && !password.trim().isEmpty()) {
                member.setPassword(password.trim());
            }
            // 儲存變更後的實體
            return memberRepository.save(member);
        }).orElse(null);
    }

    // 方法：獲取所有會員列表
    // 警告：此方法會撈取整張表，僅適合資料量極少時由管理員使用
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    // 方法：根據 ID 獲取單一會員
    // 回傳 Optional 以處理可能找不到的情況
    public Optional<Member> getMemberById(Integer id) {
        return memberRepository.findById(id);
    }
}

// ====== 備註區 ======
/*
[註1] 重大資安風險 (Security Vulnerability):
      目前的實作直接儲存並比對「明文密碼」 (Plain Text Password)。
      若資料庫外洩，所有使用者的密碼將直接暴露。
      強烈建議改進方案：
      1. 引入 Spring Security Crypto 依賴。
      2. 註冊時使用 BCrypt 加密：`member.setPassword(passwordEncoder.encode(rawPassword));`
      3. 登入時使用匹配器驗證：`passwordEncoder.matches(rawPassword, member.getPassword());`

[註2] 事務管理 (Transaction Management):
      `register` 與 `updateMember` 涉及資料寫入。
      建議加上 `@Transactional` 註解，以確保在擴充邏輯 (例如註冊後自動贈送優惠券) 時，
      若發生錯誤能正確回滾 (Rollback)，保證資料一致性。
*/