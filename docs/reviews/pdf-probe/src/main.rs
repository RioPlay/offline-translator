fn main() {
    let args: Vec<String> = std::env::args().collect();
    let depth: usize = args[2].parse().unwrap();
    let mut bytes = b"%PDF-1.4\n".to_vec();
    let catalog_offset = bytes.len();
    bytes.extend_from_slice(b"1 0 obj\n<< /Type /Catalog /Pages 2 0 R /X ");
    bytes.extend(std::iter::repeat_n(b'[', depth)); bytes.push(b'0');
    bytes.extend(std::iter::repeat_n(b']', depth));
    bytes.extend_from_slice(b" >>\nendobj\n");
    let pages_offset = bytes.len();
    bytes.extend_from_slice(b"2 0 obj\n<< /Type /Pages /Kids [] /Count 0 >>\nendobj\n");
    let xref_offset = bytes.len();
    bytes.extend_from_slice(format!("xref\n0 3\n0000000000 65535 f \n{catalog_offset:010} 00000 n \n{pages_offset:010} 00000 n \ntrailer\n<< /Size 3 /Root 1 0 R >>\nstartxref\n{xref_offset}\n%%EOF\n").as_bytes());
    let ok = if args[1] == "old" { lopdf_old::Document::load_mem(&bytes).is_ok() } else { lopdf_new::Document::load_mem(&bytes).is_ok() };
    assert!(ok); println!("parsed depth={depth}, bytes={}",bytes.len());
}
