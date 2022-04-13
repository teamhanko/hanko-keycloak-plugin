<div class="login-methods">
    <#if hasLoginMethods == true>
        <h3 class="or"><span>${msg("or")}</span></h3>
    </#if>
    <div class="stretched-row">
    <#if loginMethod != "UAF">
        <#if hasUaf == true>
            <form action="${url.loginAction}" style="display:hidden" class="${properties.kcFFormClass!}" method="post">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} btn-login-method"
                       type="submit" name="switch" id="button_cancel" value="Hanko Authenticator"/>
                <input type="hidden" name="loginMethod" value="UAF"/>
            </form>
        </#if>
    </#if>
    <#if loginMethod != "WEBAUTHN">
        <#if hasWebAuthn == true>
            <form action="${url.loginAction}" style="display:hidden" class="${properties.kcFFormClass!}" method="post">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} btn-login-method"
                       type="submit" name="switch" id="button_cancel" value="WebAuthn"/>
                <input type="hidden" name="loginMethod" value="WEBAUTHN"/>
            </form>
        </#if>
    </#if>
    </div>
</div>

<#--<div id="kc-registration">-->
<#--    <form action="${url.loginAction}" style="display:hidden" class="${properties.kcFFormClass!}" method="post">-->
<#--        <span>${msg("not")} ${username}?-->
<#--            <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!} btn-login-change-user"-->
<#--                  type="submit" name="cancel" id="button_cancel" value="${msg("changeUser")}"/>-->
<#--        </span>-->
<#--    </form>-->
<#--</div>-->