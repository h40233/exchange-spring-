package com.exchange.exchange.service;

import com.exchange.exchange.entity.Member;
import com.exchange.exchange.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    public Member register(String account, String password, String name, String number) {
        // Remove whitespace
        String cleanAccount = (account != null) ? account.trim() : "";
        String cleanPassword = (password != null) ? password.trim() : "";

        if (memberRepository.findByAccount(cleanAccount).isPresent()) {
            throw new RuntimeException("Account already exists");
        }
        Member member = new Member();
        member.setAccount(cleanAccount);
        member.setPassword(cleanPassword);
        member.setName(name);
        member.setNumber(number);
        member.setJoinTime(LocalDateTime.now());
        return memberRepository.save(member);
    }

    public Member login(String account, String password) {
        // String trimming to avoid hidden space issues
        String cleanAccount = account.trim();
        String cleanPassword = password.trim();

        System.out.println("嘗試登入 - 帳號: [" + cleanAccount + "]");

        Member member = memberRepository.findByAccount(cleanAccount).orElse(null);
        if (member == null) {
            System.out.println("登入失敗: 找不到帳號");
            throw new IllegalArgumentException("Invalid account or password");
        }
        
        // Simple string comparison
        if (member.getPassword().equals(cleanPassword)) {
            // Update session or token here if needed
            System.out.println("登入成功!");
            return member;
        } else {
            System.out.println("登入失敗: 密碼不符");
            System.out.println("輸入密碼長度: " + cleanPassword.length());
            System.out.println("儲存密碼長度: " + member.getPassword().length());
            throw new IllegalArgumentException("Invalid account or password");
        }
    }

    public Member updateMember(Integer id, String name, String number, String password) {
        return memberRepository.findById(id).map(member -> {
            if (name != null) member.setName(name);
            if (number != null) member.setNumber(number);
            if (password != null && !password.trim().isEmpty()) {
                member.setPassword(password.trim());
            }
            return memberRepository.save(member);
        }).orElse(null);
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Optional<Member> getMemberById(Integer id) {
        return memberRepository.findById(id);
    }
}