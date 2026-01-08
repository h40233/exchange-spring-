package com.exchange.exchange.controller;

// 引入實體與服務
import com.exchange.exchange.entity.Member;
import com.exchange.exchange.service.MemberService;
// 引入 Spring Web 與 Session
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

// ====== 檔案總結 ======
// MemberController 提供會員認證與資料管理的 API。
// 核心功能：
// 1. 註冊 (Register) 與 登入 (Login)。
// 2. 登出 (Logout - Invalidate Session)。
// 3. 查詢與更新個人資料 (Me)。
@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired
    private MemberService memberService;

    // API：註冊新會員
    // POST /api/members/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Member member) {
        try {
            // 呼叫 Service 進行註冊
            // 注意：這裡直接接收 Member Entity 作為參數，包含了 password 欄位
            Member newMember = memberService.register(member.getAccount(), member.getPassword(), member.getName(), member.getNumber());
            return ResponseEntity.ok(newMember);
        } catch (RuntimeException e) {
            // 帳號重複等錯誤 -> 回傳 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API：會員登入
    // POST /api/members/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Member member, HttpSession session) {
        // 呼叫 Service 驗證帳號密碼
        Member loggedInMember = memberService.login(member.getAccount(), member.getPassword());
        
        if (loggedInMember != null) {
            // 登入成功：將 memberId 寫入 Session，建立登入狀態 [註1]
            session.setAttribute("memberId", loggedInMember.getMemberId());
            return ResponseEntity.ok(loggedInMember);
        }
        // 登入失敗：回傳 401 Unauthorized
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    // API：會員登出
    // POST /api/members/logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        // 銷毀 Session，清除所有屬性
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }

    // API：獲取我的個人資料
    // GET /api/members/me
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpSession session) {
        Integer memberId = (Integer) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
        }

        Optional<Member> member = memberService.getMemberById(memberId);
        // 若找不到會員 (可能被刪除) 則回傳 404，否則回傳資料
        return member.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // API：更新我的個人資料
    // PUT /api/members/me
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody Member member, HttpSession session) {
        Integer memberId = (Integer) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
        }

        // 使用 Service 更新資料，包含選擇性的密碼變更邏輯
        // 前端若傳送 null 或空字串，Service 層會忽略該欄位
        Member updatedMember = memberService.updateMember(
                memberId,
                member.getName(),
                member.getNumber(),
                member.getPassword() 
        );

        if (updatedMember != null) {
            return ResponseEntity.ok(updatedMember);
        }
        return ResponseEntity.notFound().build();
    }
}

// ====== 備註區 ======
/*
[註1] Session 管理 (Session Management):
      目前使用 Servlet API 的 `HttpSession`。這在單體應用 (Monolith) 中是可行的。
      但在微服務或前後端分離架構中，建議改用 Token-Based 驗證 (如 JWT) 搭配 Spring Security。
      這樣可以實現無狀態 (Stateless) 驗證，更容易水平擴展。

[註2] 資料暴露 (Data Exposure):
      `register` 與 `login` 成功後直接回傳了 `Member` 物件。
      `Member` 物件中包含 `password` (即使是加密後的)。
      雖然使用了 `@JsonProperty(access = Access.WRITE_ONLY)` 來防止序列化輸出密碼 (需檢查 Entity 定義)，
      但最佳實踐仍是回傳專門的 `MemberResponseDTO`，完全排除敏感欄位。
*/