default rel
global find_abs
global find_rel
global noop

section .text

noop:
  xor rax,rax
  ret

;rsi = *key
;rdx = from
;rcx = len
find_abs:
  test edx,edx
  jle  zl3
  sub  edx,0x1
  xor  eax,eax
  add  rdx,rsi
  prefetcht0 BYTE [rdx]
  jmp  lb3
  nop  WORD [rax + rax*1 + 0x0]
inc3:
  lea  r8,[rsi+0x1]
  cmp  rdx,rsi
  je   le3
  mov  rsi,r8
lb3:
  movsxd rcx,DWORD [rdi+rax*8]
  mov    r8,rax
  movzx  eax,BYTE [rsi]
  xor    rax,rcx
  lea    rcx,[rdi+rax*8]
  cmp    DWORD [rcx+0x4],r8d
  je	 inc3
  mov    eax,0xffffffff
  ret
  nop    DWORD [rax+0x0]
zl3:
  mov rcx,rdi
  xor eax,eax
le3:
  movsxd rdx,DWORD [rcx]
  lea    rdx,[rdi+rdx*8]
  cmp    DWORD [rdx+0x4],eax
  mov    eax,0xfffffffe
  cmove  eax,DWORD [rdx]
  ret

;rdi = node*
;rsi = key*
;rdx = from
;rcx = pos
;r8  = len
;rax = rv
find_rel:
    ;endbr64
    cmp rcx,r8 ;if pos>=len go to end
    jae end ;  jump short if above or equal
    add rcx,rsi ; make shifted address u8* start = *(key + pos)
    add r8,rsi  ; make shifted address u8* limit = *(key + len)
    prefetcht0 BYTE [rcx]; since rcx is a ptr now, prefetch at the base address
    jmp loop_body
    nop DWORD [rax+0x0]
inc_pos:
    add rcx,0x1; *key_shifted++
    cmp r8,rcx;  if(pos==end)
    ;from = to (implicitly saved in rdx)
    je end
loop_body:
    movsxd rax,DWORD [rdi+rdx*8]; int base = array[from].base; mov and sign extend
    mov    rsi,rdx ; reuse register, since we'll be working with shifted addresses
    movzx  edx,BYTE [rcx] ; u64 to = (u64)key[pos]; move and zero extend
    xor    rdx,rax ; to ^= key[pos];
    lea    rax,[rdi+rdx*8]; int tmp = array[to].check
    cmp    DWORD [rax+0x4],esi ; if(tmp == (int)from)
    je     inc_pos
    mov    eax,0xffffffff; return CEDAR_NO_PATH
    ret
end:
    lea    rax,[rdi+rdx*8]; node tmp = array[from]
    movsxd rax,DWORD [rax]; int index = (int)tmp.base
    lea    rcx,[rdi+rax*8]; node n = array[index] (node n = array[array[from].base])
    mov    eax,0xfffffffe; rv = CEDAR_NO_VALUE
    cmp    DWORD [rcx+0x4],edx; if(n.check == from)
    cmove  eax, DWORD [rcx] ; rv = n.base
    ret
