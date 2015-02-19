Pod::Spec.new do |s|
  s.name     = 'LDInAppService'
  s.version  = '1.0.0'
  s.author   = { 'Imanol Fernandez' => 'support@ludei.com' }
  s.homepage = 'https://github.com/ludei/atomic-plugins-inapps'
  s.summary  = 'An easy to use, secure and functional InApp Purchase framework for iOS. Used as the core of Ludei Atomic Plugins for InApp purchases'
  s.license  = 'Apache 2'
  s.source   = { :git => 'https://github.com/ludei/atomic-plugins-inapps.git', :tag => '1.0.0' }
  s.source_files = 'src/atomic/ios/appstore'
  s.platform = :ios
  s.ios.deployment_target = '5.0'
  s.requires_arc = true
end
