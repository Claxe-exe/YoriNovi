// API/auth data is never cached; a signed-out user cannot read another account's cache.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', event => event.waitUntil(self.clients.claim()));
self.addEventListener('push', event => {
  let data;try{data=event.data.json();}catch{return;}
  event.waitUntil(self.registration.showNotification(data.title||'Fallow',{body:data.body||'',icon:'/icon.svg',badge:'/icon.svg',tag:data.eventId||'fallow',data:{url:data.url||'/'}}));
});
self.addEventListener('notificationclick', event => {
  event.notification.close();const path=event.notification.data?.url||'/';
  const url=new URL(path,self.location.origin);if(url.origin!==self.location.origin)return;
  event.waitUntil(self.clients.matchAll({type:'window',includeUncontrolled:true}).then(async clients=>{
    for(const client of clients){if('navigate' in client){await client.navigate(url.href);return client.focus();}}
    return self.clients.openWindow(url.href);
  }));
});
