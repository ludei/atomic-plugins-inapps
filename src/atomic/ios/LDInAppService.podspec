Pod::Spec.new do |s|
  s.name     = 'LDInAppService'
  s.version  = '1.0.0'
  s.author   = { 'Imanol Fernandez' => 'imanolf@ludei.com' }
  s.homepage = 'https://github.com/ludei/atomic-plugins-inapps'
  s.summary  = 'LDInAppService class provides an easy to use and secure in-app purchase API. Built-in support for local and server-side receipt validation, consumable and non-consumable purchase tracking and local products cache. Completed purchases are secured using Apple's keychain services and are remembered even if the user deletes the app'
  s.license  = 'MPL 2.0'
  s.source   = { :git => 'https://github.com/ludei/atomic-plugins-inapps.git', :tag => '1.0.1' }
  s.source_files = 'src/atomic/ios/appstore'
  s.platform = :ios
  s.ios.deployment_target = '5.0'
  s.requires_arc = true
end
