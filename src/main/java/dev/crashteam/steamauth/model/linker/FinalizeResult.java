package dev.crashteam.steamauth.model.linker;

public enum FinalizeResult {
    BadSMSCode,
    UnableToGenerateCorrectCodes,
    Success,
    GeneralFailure
}
