// Request ownership only. No identity or credential is persisted here.
let epoch = 0
export const identityEpoch = () => epoch
export function advanceIdentityEpoch() { return ++epoch }
