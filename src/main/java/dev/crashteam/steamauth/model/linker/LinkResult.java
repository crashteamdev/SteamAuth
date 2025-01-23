package dev.crashteam.steamauth.model.linker;

public enum LinkResult {
    MustProvidePhoneNumber,
    MustRemovePhoneNumber,
    MustConfirmEmail,
    AwaitingFinalization,
    GeneralFailure,
    AuthenticatorPresent,
    FailureAddingPhone
}
