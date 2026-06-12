package com.kinn.shop.user.service;

import cn.hutool.core.util.IdcardUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.user.dto.IdentityAddDTO;
import com.kinn.shop.user.entity.UserIdentity;
import com.kinn.shop.user.mapper.UserIdentityMapper;
import com.kinn.shop.user.vo.IdentityVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 清关实名：身份证号校验位校验 + AES 加密落库，仅回脱敏 mask。
 */
@Service
@RequiredArgsConstructor
public class IdentityService {

    private final UserIdentityMapper identityMapper;

    @Value("${shop.identity.aes-key:kinn-shop-dev-16}")
    private String aesKey;

    private AES aes;

    @PostConstruct
    void initAes() {
        byte[] key = aesKey.getBytes(StandardCharsets.UTF_8);
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalStateException(
                    "shop.identity.aes-key 必须为 16/24/32 字节，当前 " + key.length + " 字节");
        }
        this.aes = SecureUtil.aes(key);
    }

    public List<IdentityVO> list(long userId) {
        return identityMapper.selectList(new LambdaQueryWrapper<UserIdentity>()
                        .eq(UserIdentity::getUserId, userId)
                        .orderByDesc(UserIdentity::getIsDefault)
                        .orderByDesc(UserIdentity::getId))
                .stream().map(IdentityVO::from).toList();
    }

    @Transactional
    public IdentityVO add(long userId, IdentityAddDTO dto) {
        String idCardNo = dto.getIdCardNo().trim();
        if (!IdcardUtil.isValidCard(idCardNo)) {
            throw new BizException(ErrorCode.IDENTITY_INVALID);
        }
        boolean first = identityMapper.selectCount(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId)) == 0;
        UserIdentity identity = new UserIdentity();
        identity.setUserId(userId);
        identity.setRealName(dto.getRealName().trim());
        identity.setIdCardCipher(aes.encryptHex(idCardNo));
        identity.setIdCardMask(mask(idCardNo));
        identity.setIsDefault(first ? 1 : 0);
        identity.setVerified(1);
        identityMapper.insert(identity);
        return IdentityVO.from(identityMapper.selectById(identity.getId()));
    }

    public void delete(long userId, long id) {
        requireOwned(userId, id);
        identityMapper.deleteById(id);
    }

    /** 同一用户互斥置默认 */
    @Transactional
    public void setDefault(long userId, long id) {
        requireOwned(userId, id);
        identityMapper.update(null, new LambdaUpdateWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId)
                .eq(UserIdentity::getIsDefault, 1)
                .set(UserIdentity::getIsDefault, 0));
        UserIdentity update = new UserIdentity();
        update.setId(id);
        update.setIsDefault(1);
        identityMapper.updateById(update);
    }

    /** 前3位 + **** + 后4位 */
    private String mask(String idCardNo) {
        return idCardNo.substring(0, 3) + "****" + idCardNo.substring(idCardNo.length() - 4);
    }

    private UserIdentity requireOwned(long userId, long id) {
        UserIdentity identity = identityMapper.selectById(id);
        if (identity == null || !identity.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return identity;
    }
}
