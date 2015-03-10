Pod::Spec.new do |s|
  s.name     = 'LDInAppService'
  s.version  = '1.0.3'
  s.author   = { 'Imanol Fernandez' => 'imanolf@ludei.com' }
  s.homepage = 'https://github.com/ludei/atomic-plugins-inapps'
  s.summary  = 'LDInAppService class provides an easy to use and secure In-App Purchase API'
  s.license  = 'MPL 2.0'
  s.source   = { :git => 'https://github.com/ludei/atomic-plugins-inapps.git', :tag => '1.0.3' }
  s.source_files = 'src/atomic/ios/appstore'
  s.platform = :ios
  s.ios.deployment_target = '5.0'
  s.requires_arc = true
end
