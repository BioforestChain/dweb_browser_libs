use windows::core::{factory, HSTRING};
use windows::Foundation::IAsyncOperation;
use windows::Win32::Foundation::HWND;
use windows::Win32::System::WinRT::IUserConsentVerifierInterop;
use windows::Security::Credentials::UI::{UserConsentVerificationResult, UserConsentVerifier, UserConsentVerifierAvailability};

pub fn check_support_biometrics() -> i8 {
	// https://learn.microsoft.com/en-us/uwp/api/windows.security.credentials.ui.userconsentverifier.checkavailabilityasync?view=winrt-22621
    let ucv_result = UserConsentVerifier::CheckAvailabilityAsync();

	match ucv_result {
		Ok(uct_opt) => match uct_opt.get() {
			// https://learn.microsoft.com/zh-cn/uwp/api/windows.security.credentials.ui.userconsentverifieravailability?view=winrt-22621
			Ok(ucv_available) => match ucv_available {
				UserConsentVerifierAvailability::Available => 0, // 生物识别验证程序设备可用 0
				UserConsentVerifierAvailability::DeviceBusy => 1, // 生物识别验证程序设备正在执行操作，并且不可用 4
				UserConsentVerifierAvailability::NotConfiguredForUser => 11, // 未为此用户配置生物识别验证程序设备 2
				UserConsentVerifierAvailability::DeviceNotPresent => 12, // 没有可用的生物识别验证程序设备 1
				UserConsentVerifierAvailability::DisabledByPolicy => -1, // 组策略已禁用生物识别验证程序设备 3
				_ => -1
			},
			Err(_) => -1
		},
		Err(_) => -1
	}
}

// 参考：https://github.com/bitwarden/clients/blob/d28634b06882feee3cf6bb9b2f1e5e5aea91eac8/apps/desktop/desktop_native/src/biometric/windows.rs#L48
pub fn biometrics_result_content(reason: String) -> (bool, String) {
	let interop = factory::<UserConsentVerifier, IUserConsentVerifierInterop>().unwrap();

	// https://learn.microsoft.com/en-us/uwp/api/windows.security.credentials.ui.userconsentverifier.requestverificationasync?view=winrt-22621
	let operation: IAsyncOperation<UserConsentVerificationResult> = unsafe {
		interop.RequestVerificationForWindowAsync(HWND(0), &HSTRING::from(reason)).unwrap()
	};

	match operation.get() {
		// https://learn.microsoft.com/zh-cn/uwp/api/windows.security.credentials.ui.userconsentverificationresult?view=winrt-22621
		Ok(result) => match result {
			UserConsentVerificationResult::Verified => (true, "".to_string()),
			_ => (false, "There is no biometric verifier device available.".to_string())
		}
		Err(_) => (false, "".to_string())
	}
}
