typedef unsigned int  u32;
typedef unsigned long u64;

extern int find_rel (u64 array, u64 key, u64 from, u64 pos, u64 len);

extern int find_abs (u64 array, u64 key, u32 len);

extern int noop(u64 array, u64 key, u32 len);
